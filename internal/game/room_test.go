package game

import (
	"bytes"
	"encoding/binary"
	"encoding/json"
	"strconv"
	"testing"
	"time"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/dict"
)

const (
	starterID = int64(1)
	invitedID = int64(2)
	moveTime  = 92 * time.Second
)

type fakeSeat struct {
	id   int64
	msgs [][]byte
}

func (s *fakeSeat) UserID() int64 { return s.id }

func (s *fakeSeat) Send(msg []byte) { s.msgs = append(s.msgs, msg) }

func (s *fakeSeat) last(t *testing.T) map[string]any {
	t.Helper()

	if len(s.msgs) == 0 {
		t.Fatalf("seat %d: no messages", s.id)
	}

	var m map[string]any
	if err := json.Unmarshal(s.msgs[len(s.msgs)-1], &m); err != nil {
		t.Fatal(err)
	}

	return m
}

func buildDict(t *testing.T, names ...string) *dict.Dictionary {
	t.Helper()

	var buf bytes.Buffer

	record := func(name string) {
		var enc []byte

		for _, r := range name {
			if r < 127 {
				enc = append(enc, byte(r))
			} else {
				enc = append(enc, byte(r-(0x0430-127)))
			}
		}

		buf.WriteByte(byte(len(enc)))
		buf.WriteByte(0)
		buf.Write([]byte{0, 0})
		buf.Write(enc)
	}

	_ = binary.Write(&buf, binary.BigEndian, uint16(0xFED0))
	_ = binary.Write(&buf, binary.BigEndian, uint16(1))
	_ = binary.Write(&buf, binary.BigEndian, uint32(1))
	_ = binary.Write(&buf, binary.BigEndian, uint32(len(names)))

	for _, n := range names {
		record(n)
	}

	record(strconv.Itoa(len(names)) + "abcdef")

	d, err := dict.Parse(&buf)
	if err != nil {
		t.Fatal(err)
	}

	return d
}

type fixture struct {
	room    *Room
	starter *fakeSeat
	invited *fakeSeat
	clk     *FakeClock
	timers  []int
}

func newFixture(t *testing.T) *fixture {
	t.Helper()

	f := &fixture{
		starter: &fakeSeat{id: starterID},
		invited: &fakeSeat{id: invitedID},
		clk:     NewFakeClock(time.Unix(1000, 0)),
	}
	d := buildDict(t, "москва", "анапа", "абакан", "тверь", "рязань")
	f.room = NewRoom(7, f.starter, f.invited,
		Opponent{ID: starterID, Name: "A", Friendship: FriendshipNone},
		Opponent{ID: invitedID, Name: "B", Friendship: FriendshipNone},
		d, moveTime, f.clk, func(_ int64, move int) { f.timers = append(f.timers, move) })
	f.room.Start()

	return f
}

func wordMsg(t *testing.T, s *fakeSeat, by int64, text, status string) {
	t.Helper()

	m := s.last(t)
	if m["type"] != "word" || int64(m["by"].(float64)) != by || m["text"] != text || m["status"] != status {
		t.Fatalf("seat %d: got %v, want word by=%d %q %s", s.id, m, by, text, status)
	}
}

func TestRoomStart(t *testing.T) {
	f := newFixture(t)

	a, b := f.starter.last(t), f.invited.last(t)
	if a["type"] != "match" || a["youStart"] != true || a["moveTimeSec"].(float64) != 92 {
		t.Fatalf("starter match: %v", a)
	}

	if b["youStart"] != false || b["opponent"].(map[string]any)["name"] != "A" {
		t.Fatalf("invited match: %v", b)
	}
}

func TestRoomFlow(t *testing.T) {
	f := newFixture(t)

	for _, step := range []struct {
		from int64
		word string
	}{{starterID, "Москва"}, {invitedID, "анапа"}, {starterID, "абакан"}} {
		f.room.Word(step.from, step.word)

		wordMsg(t, f.starter, step.from, Normalize(step.word), WordAccepted)
		wordMsg(t, f.invited, step.from, Normalize(step.word), WordAccepted)
	}
}

type step struct {
	from int64
	word string
}

func TestRoomRejections(t *testing.T) {
	tests := []struct {
		name   string
		setup  []step
		try    step
		next   step
		status string
	}{
		{"not your turn", nil, step{invitedID, "москва"}, step{starterID, "москва"}, WordNotYourTurn},
		{"wrong letter", []step{{starterID, "москва"}}, step{invitedID, "рязань"}, step{invitedID, "анапа"}, WordWrongLetter},
		{"already used", []step{{starterID, "москва"}, {invitedID, "анапа"}}, step{starterID, "анапа"}, step{starterID, "абакан"}, WordAlreadyUsed},
		{"unknown", []step{{starterID, "москва"}}, step{invitedID, "абвгд"}, step{invitedID, "анапа"}, WordUnknown},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			f := newFixture(t)

			for _, s := range tt.setup {
				f.room.Word(s.from, s.word)
			}

			sender, other := f.starter, f.invited
			if tt.try.from == invitedID {
				sender, other = f.invited, f.starter
			}

			nSender, nOther := len(sender.msgs), len(other.msgs)

			f.room.Word(tt.try.from, tt.try.word)

			if len(sender.msgs) != nSender+1 || len(other.msgs) != nOther {
				t.Fatalf("rejection must reach only the sender")
			}

			wordMsg(t, sender, tt.try.from, Normalize(tt.try.word), tt.status)

			// the turn did not change: the expected player can still move
			seat := f.starter
			if tt.next.from == invitedID {
				seat = f.invited
			}

			f.room.Word(tt.next.from, tt.next.word)
			wordMsg(t, seat, tt.next.from, tt.next.word, WordAccepted)
		})
	}
}

func TestRoomNonParticipantIgnored(t *testing.T) {
	f := newFixture(t)

	f.room.Word(99, "москва")

	if len(f.starter.msgs) != 1 || len(f.invited.msgs) != 1 {
		t.Fatal("outsider word must be ignored")
	}
}

func TestRoomLastLetterSkipsSoft(t *testing.T) {
	f := newFixture(t)
	d := buildDict(t, "тверь", "рязань", "москва")
	r := NewRoom(1, f.starter, f.invited, Opponent{}, Opponent{}, d, moveTime, f.clk, func(int64, int) {})
	r.Start()
	r.Word(starterID, "тверь")
	r.Word(invitedID, "москва")
	wordMsg(t, f.invited, invitedID, "москва", WordWrongLetter)

	r.Word(invitedID, "рязань")
	wordMsg(t, f.invited, invitedID, "рязань", WordAccepted)
}

func TestRoomTimeout(t *testing.T) {
	f := newFixture(t)
	f.room.Word(starterID, "москва")
	f.clk.Advance(91 * time.Second)

	if len(f.timers) != 0 {
		t.Fatal("timer fired early")
	}

	f.clk.Advance(2 * time.Second)

	if len(f.timers) != 1 {
		t.Fatalf("onTimer calls: %v", f.timers)
	}

	if !f.room.Timer(f.timers[0]) {
		t.Fatal("Timer must finish the game")
	}

	for _, s := range []*fakeSeat{f.starter, f.invited} {
		m := s.last(t)
		if m["type"] != "gameOver" || m["reason"] != ReasonTimeout || int64(m["loser"].(float64)) != invitedID {
			t.Fatalf("gameOver: %v", m)
		}

		if m["words"].(float64) != 1 || m["durationSec"].(float64) != 93 {
			t.Fatalf("gameOver stats: %v", m)
		}
	}
}

func TestRoomStaleTimer(t *testing.T) {
	f := newFixture(t)
	f.clk.Advance(moveTime)
	stale := f.timers[0]
	f.room.Word(starterID, "москва")

	n := len(f.starter.msgs)
	if f.room.Timer(stale) {
		t.Fatal("stale timer finished the game")
	}

	if len(f.starter.msgs) != n || len(f.invited.msgs) != n {
		t.Fatal("stale timer sent messages")
	}
}

func TestRoomTimerRearmed(t *testing.T) {
	f := newFixture(t)
	f.clk.Advance(60 * time.Second)
	f.room.Word(starterID, "москва")
	f.clk.Advance(60 * time.Second)

	if len(f.timers) != 0 {
		t.Fatal("old timer was not stopped")
	}

	f.clk.Advance(32 * time.Second)

	if len(f.timers) != 1 || f.timers[0] != 1 {
		t.Fatalf("timers: %v", f.timers)
	}
}

func TestRoomLeave(t *testing.T) {
	f := newFixture(t)
	f.room.Leave(starterID, ReasonOpponentLeft)

	for _, s := range []*fakeSeat{f.starter, f.invited} {
		m := s.last(t)
		if m["reason"] != ReasonOpponentLeft || int64(m["loser"].(float64)) != starterID {
			t.Fatalf("gameOver: %v", m)
		}
	}
}

func TestRoomFinishedIsFrozen(t *testing.T) {
	f := newFixture(t)
	f.room.Word(starterID, "москва")
	f.room.Leave(invitedID, ReasonSurrender)

	n := len(f.starter.msgs)
	g1, ok1 := f.room.Result()

	f.clk.Advance(time.Hour)

	f.room.Word(invitedID, "анапа")

	f.room.Chat(invitedID, "hi", false)
	f.room.Leave(starterID, ReasonSurrender)

	if f.room.Timer(1) || len(f.starter.msgs) != n || len(f.timers) != 0 {
		t.Fatal("finished room must be a no-op")
	}

	g2, ok2 := f.room.Result()
	if g1 != g2 || ok1 != ok2 {
		t.Fatalf("result changed: %v %v", g1, g2)
	}
}

func TestRoomResult(t *testing.T) {
	f := newFixture(t)

	if _, ok := f.room.Result(); ok {
		t.Fatal("no words must not be recorded")
	}

	f.clk.Advance(10 * time.Second)
	f.room.Word(starterID, "москва")
	f.clk.Advance(20 * time.Second)
	f.room.Word(invitedID, "анапа")
	f.clk.Advance(5 * time.Second)
	f.room.Leave(starterID, ReasonSurrender)

	g, ok := f.room.Result()
	if !ok || g.Words != 2 || g.Duration != 35*time.Second {
		t.Fatalf("result: %+v %v", g, ok)
	}

	if g.StarterID != starterID || g.InvitedID != invitedID || !g.StartedAt.Equal(time.Unix(1000, 0)) {
		t.Fatalf("result: %+v", g)
	}
}

func TestRoomChat(t *testing.T) {
	f := newFixture(t)
	f.room.Chat(starterID, "hi", true)

	if len(f.starter.msgs) != 1 || len(f.invited.msgs) != 1 {
		t.Fatal("blocked chat was delivered")
	}

	f.room.Chat(starterID, "hi", false)

	m := f.invited.last(t)
	if m["type"] != "chat" || m["text"] != "hi" || int64(m["by"].(float64)) != starterID || len(f.starter.msgs) != 1 {
		t.Fatalf("chat: %v", m)
	}
}

package game

import (
	"math/rand"
	"os"
	"path/filepath"
	"slices"
	"strconv"
	"testing"
	"time"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

const (
	bot1 = int64(12407)
	bot2 = int64(12408)

	botsWait = 15 * time.Second
)

var botIDs = []int64{12407, 12408, 14397, 14398, 14399, 14400}

// newBotFixture is a hub fixture with the given bots registered. Every bot
// answers 3 to 5 seconds after the opponent's move.
func newBotFixture(t *testing.T, enabled bool, ids ...int64) *hubFixture {
	t.Helper()

	f := newHubFixture(t)
	f.hub.cfg.BotsEnabled = enabled

	for _, id := range ids {
		f.st.users[id] = store.User{ID: id, Name: "Bot " + strconv.FormatInt(id, 10)}
		spec := BotSpec{UserID: id, Level: 2, MinDelaySec: 3, MaxDelaySec: 5}
		f.hub.AddBot(NewBot(spec, f.hub, f.hub.dict, f.clk, rand.New(rand.NewSource(id))))
	}

	f.hub.drain()

	return f
}

func (f *hubFixture) advance(d time.Duration) {
	f.clk.Advance(d)
	f.hub.drain()
}

// playBot logs in id, queues it and lets the bot wait expire.
func (f *hubFixture) playBot(id, bot int64) *fakeConn {
	c := f.login(id)
	f.send(id, ClientMsg{Type: TypeFind})
	f.advance(botsWait)
	wantMatch(f.t, c, bot, true)

	return c
}

func wantWord(t *testing.T, c *fakeConn, by int64, text string) {
	t.Helper()

	m := wantType(t, c, TypeWord)
	if m["by"] != float64(by) || m["text"] != text || m["status"] != WordAccepted {
		t.Fatalf("want word %q accepted by %d, got %v", text, by, m)
	}
}

func TestBotJoinsAfterWait(t *testing.T) {
	f := newBotFixture(t, true, botIDs...)
	ca := f.login(userA)

	f.send(userA, ClientMsg{Type: TypeFind})
	f.advance(14 * time.Second)

	wantType(t, ca, TypeQueued)

	f.advance(time.Second)

	m := wantMatch(t, ca, bot1, true)
	if o, _ := m["opponent"].(map[string]any); o["name"] != "Bot 12407" {
		t.Fatalf("opponent: %v", o)
	}
}

func TestBotSkippedWhenBlocked(t *testing.T) {
	f := newBotFixture(t, true, botIDs...)
	f.st.block(userA, bot1)

	f.playBot(userA, bot2)
}

func TestBotNoFreeBots(t *testing.T) {
	f := newBotFixture(t, true, bot1)
	f.playBot(userB, bot1)

	ca := f.login(userA)
	f.send(userA, ClientMsg{Type: TypeFind})
	f.advance(botsWait)

	wantType(t, ca, TypeQueued)

	cc := f.login(userC)
	f.send(userC, ClientMsg{Type: TypeFind})

	wantMatch(t, ca, userC, true)
	wantMatch(t, cc, userA, false)
}

func TestBotWaitRestartsOnRequeue(t *testing.T) {
	f := newBotFixture(t, true, botIDs...)
	ca := f.login(userA)

	f.send(userA, ClientMsg{Type: TypeFind})
	f.advance(10 * time.Second)
	f.send(userA, ClientMsg{Type: TypeCancel})
	f.send(userA, ClientMsg{Type: TypeFind})
	f.advance(14 * time.Second)

	wantType(t, ca, TypeQueued)

	f.advance(time.Second)

	wantMatch(t, ca, bot1, true)
}

func TestBotAnswers(t *testing.T) {
	f := newBotFixture(t, true, botIDs...)
	ca := f.playBot(userA, bot1)

	f.send(userA, ClientMsg{Type: TypeWord, Text: "москва"})

	wantWord(t, ca, userA, "москва")

	f.advance(2 * time.Second)

	wantWord(t, ca, userA, "москва")

	f.advance(3 * time.Second)

	wantWord(t, ca, bot1, "анапа")
}

func TestBotWithoutWordLosesAndIsFreed(t *testing.T) {
	f := newBotFixture(t, true, bot1)
	ca := f.playBot(userA, bot1)

	f.send(userA, ClientMsg{Type: TypeWord, Text: "анапа"})
	f.advance(moveTime)

	wantGameOver(t, ca, ReasonTimeout, bot1)

	if len(f.st.games) != 1 || f.st.games[0].StarterID != userA || f.st.games[0].InvitedID != bot1 {
		t.Fatalf("games: %+v", f.st.games)
	}

	f.playBot(userB, bot1)
}

func TestBotMoveCancelledOnGameOver(t *testing.T) {
	f := newBotFixture(t, true, bot1)
	ca := f.playBot(userA, bot1)

	f.send(userA, ClientMsg{Type: TypeWord, Text: "москва"})
	f.send(userA, ClientMsg{Type: TypeLeave})

	wantGameOver(t, ca, ReasonSurrender, userA)

	f.clk.Advance(10 * time.Second)

	if n := len(f.hub.events); n != 0 {
		t.Fatalf("bot moved after game over: %d events", n)
	}
}

func TestInviteBotOffline(t *testing.T) {
	f := newBotFixture(t, true, bot1)
	f.st.befriend(userA, bot1)
	ca := f.login(userA)

	f.send(userA, ClientMsg{Type: TypeInvite, UserID: bot1})

	wantInviteStatus(t, ca, bot1, InviteOffline)
}

func TestBotsDisabled(t *testing.T) {
	f := newBotFixture(t, false, botIDs...)
	ca := f.login(userA)

	f.send(userA, ClientMsg{Type: TypeFind})
	f.advance(60 * time.Second)

	wantType(t, ca, TypeQueued)
}

func TestLoadBots(t *testing.T) {
	bots, err := LoadBots(filepath.Join("..", "..", "testdata", "bots.json"))
	if err != nil {
		t.Fatal(err)
	}

	ids := make([]int64, 0, len(bots))

	for _, b := range bots {
		ids = append(ids, b.UserID)

		if b.Level > 3 || b.MinDelaySec < 0 || b.MaxDelaySec < b.MinDelaySec {
			t.Errorf("bad spec: %+v", b)
		}
	}

	if !slices.Equal(ids, botIDs) {
		t.Fatalf("ids: %v", ids)
	}
}

func TestLoadBotsInvalid(t *testing.T) {
	dir := t.TempDir()
	cases := []string{
		`[{"userId":`,
		`[{"level":1,"minDelaySec":3,"maxDelaySec":5}]`,
		`[{"userId":1,"minDelaySec":-1,"maxDelaySec":5}]`,
		`[{"userId":1,"minDelaySec":5,"maxDelaySec":3}]`,
	}

	for i, body := range cases {
		path := filepath.Join(dir, strconv.Itoa(i)+".json")
		if err := os.WriteFile(path, []byte(body), 0o600); err != nil {
			t.Fatal(err)
		}

		if _, err := LoadBots(path); err == nil {
			t.Errorf("case %s: want error", body)
		}
	}

	if _, err := LoadBots(filepath.Join(dir, "missing.json")); err == nil {
		t.Error("missing file: want error")
	}
}

package game

import (
	"context"
	"errors"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

const (
	userA = int64(1)
	userB = int64(2)
	userC = int64(3)
	userD = int64(4)

	inviteTTL = 60 * time.Second
)

var errStore = errors.New("store is down")

type fakeConn struct {
	msgs   [][]byte
	closed []string
	onSend func(msg []byte)
}

func (c *fakeConn) Send(msg []byte) {
	c.msgs = append(c.msgs, msg)

	if c.onSend != nil {
		c.onSend(msg)
	}
}

func (c *fakeConn) Close(code string) { c.closed = append(c.closed, code) }

func (c *fakeConn) last(t *testing.T) map[string]any {
	t.Helper()

	if len(c.msgs) == 0 {
		t.Fatal("conn: no messages")
	}

	return toMap(t, c.msgs[len(c.msgs)-1])
}

func (c *fakeConn) count(typ string) int {
	n := 0

	for _, b := range c.msgs {
		if strings.Contains(string(b), `"type":"`+typ+`"`) {
			n++
		}
	}

	return n
}

type relKey struct{ viewer, other int64 }

type fakeStore struct {
	mu    sync.Mutex
	users map[int64]store.User
	rels  map[relKey]store.Relation
	games []store.Game
	err   error
}

func (s *fakeStore) User(_ context.Context, id int64) (store.User, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if s.err != nil {
		return store.User{}, s.err
	}

	u, ok := s.users[id]
	if !ok {
		return store.User{}, store.ErrNotFound
	}

	return u, nil
}

func (s *fakeStore) Relation(_ context.Context, viewer, other int64) (store.Relation, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if s.err != nil {
		return store.Relation{}, s.err
	}

	r, ok := s.rels[relKey{viewer, other}]
	if !ok {
		return store.Relation{Friendship: store.FriendNone, Blocked: store.BlockedNone}, nil
	}

	return r, nil
}

func (s *fakeStore) RecordGame(_ context.Context, g store.Game) (int64, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if s.err != nil {
		return 0, s.err
	}

	s.games = append(s.games, g)

	return int64(len(s.games)), nil
}

func (s *fakeStore) befriend(a, b int64) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.rels[relKey{a, b}] = store.Relation{Friendship: store.FriendFriends, Blocked: store.BlockedNone}
	s.rels[relKey{b, a}] = store.Relation{Friendship: store.FriendFriends, Blocked: store.BlockedNone}
}

func (s *fakeStore) block(a, b int64) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.rels[relKey{a, b}] = store.Relation{Friendship: store.FriendNone, Blocked: store.BlockedByMe}
	s.rels[relKey{b, a}] = store.Relation{Friendship: store.FriendNone, Blocked: store.BlockedMe}
}

type pushCall struct {
	token string
	data  map[string]string
}

type fakePush struct {
	calls []pushCall
	err   error
}

func (p *fakePush) Send(_ context.Context, token string, data map[string]string) error {
	p.calls = append(p.calls, pushCall{token: token, data: data})

	return p.err
}

type hubFixture struct {
	t     *testing.T
	hub   *Hub
	st    *fakeStore
	push  *fakePush
	clk   *FakeClock
	conns map[int64]*fakeConn
}

func newHubFixture(t *testing.T) *hubFixture {
	t.Helper()

	st := &fakeStore{users: map[int64]store.User{}, rels: map[relKey]store.Relation{}}
	for id, name := range map[int64]string{userA: "A", userB: "B", userC: "C", userD: "D"} {
		st.users[id] = store.User{ID: id, Name: name, FCMToken: "token-" + name}
	}

	f := &hubFixture{
		t:     t,
		st:    st,
		push:  &fakePush{},
		clk:   NewFakeClock(time.Unix(1000, 0)),
		conns: map[int64]*fakeConn{},
	}
	cfg := HubConfig{MoveTime: moveTime, InviteTTL: inviteTTL, BotsWait: 15 * time.Second}
	f.hub = NewHub(cfg, st, buildDict(t, "москва", "анапа"), f.push, f.clk)

	return f
}

func (f *hubFixture) connect(id int64) *fakeConn {
	c := &fakeConn{}
	f.conns[id] = c
	f.hub.Connect(f.st.users[id], c)
	f.hub.drain()

	return c
}

func (f *hubFixture) send(id int64, m ClientMsg) {
	f.hub.Message(id, f.conns[id], m)
	f.hub.drain()
}

func (f *hubFixture) login(id int64) *fakeConn {
	c := f.connect(id)
	f.send(id, ClientMsg{Type: TypeHello, Protocol: 1, Client: "test"})

	if got := c.last(f.t); got["type"] != TypeWelcome {
		f.t.Fatalf("user %d: want welcome, got %v", id, got)
	}

	return c
}

func (f *hubFixture) disconnect(id int64) {
	f.hub.Disconnect(id, f.conns[id])
	f.hub.drain()
}

// play logs in a and b and matches them randomly; a starts.
func (f *hubFixture) play(a, b int64) (*fakeConn, *fakeConn) {
	ca, cb := f.login(a), f.login(b)
	f.send(a, ClientMsg{Type: TypeFind})
	f.send(b, ClientMsg{Type: TypeFind})
	wantMatch(f.t, ca, b, true)
	wantMatch(f.t, cb, a, false)

	return ca, cb
}

func (f *hubFixture) invite(from, to int64) {
	f.send(from, ClientMsg{Type: TypeInvite, UserID: to})
	wantInviteStatus(f.t, f.conns[from], to, InviteSent)
}

func wantError(t *testing.T, c *fakeConn, code string) {
	t.Helper()

	if m := c.last(t); m["type"] != TypeError || m["code"] != code {
		t.Fatalf("want error %s, got %v", code, m)
	}
}

func wantType(t *testing.T, c *fakeConn, typ string) map[string]any {
	t.Helper()

	m := c.last(t)
	if m["type"] != typ {
		t.Fatalf("want %s, got %v", typ, m)
	}

	return m
}

func wantMatch(t *testing.T, c *fakeConn, opp int64, youStart bool) map[string]any {
	t.Helper()

	m := wantType(t, c, TypeMatch)

	o, _ := m["opponent"].(map[string]any)
	if o["id"] != float64(opp) || m["youStart"] != youStart || m["moveTimeSec"] != float64(92) {
		t.Fatalf("want match vs %d youStart=%v, got %v", opp, youStart, m)
	}

	return m
}

func wantInviteStatus(t *testing.T, c *fakeConn, userID int64, status string) {
	t.Helper()

	m := wantType(t, c, TypeInviteStatus)
	if m["userId"] != float64(userID) || m["status"] != status {
		t.Fatalf("want inviteStatus %d %s, got %v", userID, status, m)
	}
}

func wantGameOver(t *testing.T, c *fakeConn, reason string, loser int64) {
	t.Helper()

	m := wantType(t, c, TypeGameOver)
	if m["reason"] != reason || m["loser"] != float64(loser) {
		t.Fatalf("want gameOver %s loser %d, got %v", reason, loser, m)
	}
}

func TestHelloRequired(t *testing.T) {
	f := newHubFixture(t)
	c := f.connect(userA)

	f.send(userA, ClientMsg{Type: TypeFind})

	wantError(t, c, ErrCodeHelloRequired)

	f.send(userA, ClientMsg{Type: TypeHello, Protocol: 1})

	m := wantType(t, c, TypeWelcome)
	if u, _ := m["user"].(map[string]any); u["id"] != float64(userA) || u["name"] != "A" || u["avatarHash"] != nil {
		t.Fatalf("welcome user: %v", m)
	}

	f.send(userA, ClientMsg{Type: TypeHello, Protocol: 1})

	wantError(t, c, ErrCodeInvalidState)
}

func TestUnsupportedProtocol(t *testing.T) {
	f := newHubFixture(t)
	c := f.connect(userA)

	f.send(userA, ClientMsg{Type: TypeHello, Protocol: 2})

	wantError(t, c, ErrCodeUnsupportedProtocol)

	if len(c.closed) != 1 || c.closed[0] != ErrCodeUnsupportedProtocol {
		t.Fatalf("closed: %v", c.closed)
	}
}

func TestRandomMatch(t *testing.T) {
	f := newHubFixture(t)
	ca, cb := f.login(userA), f.login(userB)

	f.send(userA, ClientMsg{Type: TypeFind})

	wantType(t, ca, TypeQueued)

	f.send(userB, ClientMsg{Type: TypeFind})

	ma := wantMatch(t, ca, userB, true)
	mb := wantMatch(t, cb, userA, false)

	if cb.count(TypeQueued) != 1 {
		t.Fatalf("B must be queued before the match: %d", cb.count(TypeQueued))
	}

	if ma["gameId"] != mb["gameId"] {
		t.Fatalf("game ids differ: %v %v", ma, mb)
	}

	if o, _ := ma["opponent"].(map[string]any); o["name"] != "B" || o["friendship"] != FriendshipNone {
		t.Fatalf("opponent: %v", o)
	}
}

func TestRandomSkipsBlocked(t *testing.T) {
	f := newHubFixture(t)
	f.st.block(userA, userB)
	ca, cb, cc := f.login(userA), f.login(userB), f.login(userC)

	f.send(userA, ClientMsg{Type: TypeFind})
	f.send(userB, ClientMsg{Type: TypeFind})

	wantType(t, cb, TypeQueued)

	f.send(userC, ClientMsg{Type: TypeFind})

	wantMatch(t, ca, userC, true)
	wantMatch(t, cc, userA, false)
	wantType(t, cb, TypeQueued)

	cd := f.login(userD)
	f.send(userD, ClientMsg{Type: TypeFind})

	wantMatch(t, cb, userD, true)
	wantMatch(t, cd, userB, false)
}

func TestInvalidState(t *testing.T) {
	f := newHubFixture(t)
	ca, cb := f.play(userA, userB)

	f.send(userA, ClientMsg{Type: TypeFind})

	wantError(t, ca, ErrCodeInvalidState)

	f.send(userA, ClientMsg{Type: TypeWord, Text: "москва"})

	if m := wantType(t, cb, TypeWord); m["status"] != WordAccepted {
		t.Fatalf("A must still be playing: %v", m)
	}

	cc := f.login(userC)
	f.send(userC, ClientMsg{Type: TypeFind})
	f.send(userC, ClientMsg{Type: TypeFind})

	wantError(t, cc, ErrCodeInvalidState)

	cd := f.login(userD)
	for _, typ := range []string{TypeWord, TypeChat, TypeLeave, TypeCancel} {
		f.send(userD, ClientMsg{Type: typ, Text: "москва"})

		wantError(t, cd, ErrCodeInvalidState)
	}

	f.send(userD, ClientMsg{Type: TypeFind})

	wantMatch(t, cc, userD, true)
	wantMatch(t, cd, userC, false)
}

func TestInviteOrder(t *testing.T) {
	tests := []struct {
		name    string
		setup   func(f *hubFixture)
		status  string
		pushes  int
		pushErr error
	}{
		{"blocked", func(f *hubFixture) { f.st.block(userB, userA) }, InviteBlocked, 0, nil},
		{"notFriends", func(*hubFixture) {}, InviteNotFriends, 0, nil},
		{"busy", func(f *hubFixture) {
			f.st.befriend(userA, userB)
			f.play(userB, userC)
		}, InviteBusy, 0, nil},
		{"noToken", func(f *hubFixture) {
			f.st.befriend(userA, userB)
			f.st.users[userB] = store.User{ID: userB, Name: "B"}
		}, InviteOffline, 0, nil},
		{"pushError", func(f *hubFixture) { f.st.befriend(userA, userB) }, InviteOffline, 1, errors.New("fcm down")},
		{"sent", func(f *hubFixture) { f.st.befriend(userA, userB) }, InviteSent, 1, nil},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			f := newHubFixture(t)
			f.push.err = tt.pushErr
			ca := f.login(userA)
			tt.setup(f)

			f.send(userA, ClientMsg{Type: TypeInvite, UserID: userB})

			wantInviteStatus(t, ca, userB, tt.status)

			if len(f.push.calls) != tt.pushes {
				t.Fatalf("pushes: %v", f.push.calls)
			}

			if tt.status != InviteSent {
				f.send(userA, ClientMsg{Type: TypeFind})
				wantType(t, ca, TypeQueued)

				return
			}

			call := f.push.calls[0]
			if call.token != "token-B" || len(call.data) != 3 || call.data["type"] != "invite" ||
				call.data["fromId"] != "1" || call.data["fromName"] != "A" {
				t.Fatalf("push: %v", call)
			}

			f.send(userA, ClientMsg{Type: TypeInvite, UserID: userB})
			wantError(t, ca, ErrCodeInvalidState)
		})
	}
}

func TestInviteStoreError(t *testing.T) {
	f := newHubFixture(t)
	ca := f.login(userA)
	f.st.err = errStore

	f.send(userA, ClientMsg{Type: TypeInvite, UserID: userB})

	wantError(t, ca, ErrCodeInvalidState)

	f.st.err = nil
	f.send(userA, ClientMsg{Type: TypeFind})

	wantType(t, ca, TypeQueued)
}

func TestInviteAcceptDecline(t *testing.T) {
	t.Run("accept", func(t *testing.T) {
		f := newHubFixture(t)
		f.st.befriend(userA, userB)
		ca := f.login(userA)
		f.invite(userA, userB)
		cb := f.login(userB)

		f.send(userB, ClientMsg{Type: TypeInviteReply, UserID: userA, Accept: true})

		ma := wantMatch(t, ca, userB, true)
		wantMatch(t, cb, userA, false)

		if o, _ := ma["opponent"].(map[string]any); o["friendship"] != FriendshipFriends {
			t.Fatalf("opponent: %v", o)
		}

		f.clk.Advance(inviteTTL)
		f.hub.drain()

		if ca.count(TypeInviteStatus) != 1 {
			t.Fatalf("accepted invite must not expire: %d", ca.count(TypeInviteStatus))
		}
	})

	t.Run("decline", func(t *testing.T) {
		f := newHubFixture(t)
		f.st.befriend(userA, userB)
		ca := f.login(userA)
		f.invite(userA, userB)
		f.login(userB)

		f.send(userB, ClientMsg{Type: TypeInviteReply, UserID: userA})

		wantInviteStatus(t, ca, userB, InviteDeclined)

		f.send(userA, ClientMsg{Type: TypeFind})

		wantType(t, ca, TypeQueued)
	})
}

func TestInviteAcceptWhileQueued(t *testing.T) {
	f := newHubFixture(t)
	f.st.befriend(userA, userB)
	ca, cb := f.login(userA), f.login(userB)
	f.invite(userA, userB)
	f.send(userB, ClientMsg{Type: TypeFind})

	f.send(userB, ClientMsg{Type: TypeInviteReply, UserID: userA, Accept: true})

	wantMatch(t, ca, userB, true)
	wantMatch(t, cb, userA, false)

	cc := f.login(userC)
	f.send(userC, ClientMsg{Type: TypeFind})

	wantType(t, cc, TypeQueued)
}

func TestInviteBlockedBeforeAccept(t *testing.T) {
	f := newHubFixture(t)
	f.st.befriend(userA, userB)
	ca := f.login(userA)
	f.invite(userA, userB)
	cb := f.login(userB)
	f.st.block(userB, userA)

	f.send(userB, ClientMsg{Type: TypeInviteReply, UserID: userA, Accept: true})

	wantInviteStatus(t, cb, userA, InviteBlocked)
	wantInviteStatus(t, ca, userB, InviteBlocked)

	if ca.count(TypeMatch)+cb.count(TypeMatch) != 0 {
		t.Fatal("blocked players were matched")
	}

	f.send(userA, ClientMsg{Type: TypeFind})

	wantType(t, ca, TypeQueued)
}

func TestInviteExpired(t *testing.T) {
	f := newHubFixture(t)
	f.st.befriend(userA, userB)
	ca := f.login(userA)
	f.invite(userA, userB)
	cb := f.login(userB)

	f.clk.Advance(inviteTTL)
	f.hub.drain()

	wantInviteStatus(t, ca, userB, InviteExpired)

	f.send(userB, ClientMsg{Type: TypeInviteReply, UserID: userA, Accept: true})

	wantInviteStatus(t, cb, userA, InviteExpired)

	if ca.count(TypeMatch) != 0 {
		t.Fatal("expired invite started a game")
	}
}

func TestInviteCancelledByDisconnect(t *testing.T) {
	for _, withdraw := range []string{"disconnect", TypeCancel, TypeFind} {
		t.Run(withdraw, func(t *testing.T) {
			f := newHubFixture(t)
			f.st.befriend(userA, userB)
			ca := f.login(userA)
			f.invite(userA, userB)
			cb := f.login(userB)

			if withdraw == "disconnect" {
				f.disconnect(userA)
			} else {
				f.send(userA, ClientMsg{Type: withdraw})
			}

			f.send(userB, ClientMsg{Type: TypeInviteReply, UserID: userA, Accept: true})

			wantInviteStatus(t, cb, userA, InviteExpired)

			if ca.count(TypeMatch) != 0 {
				t.Fatal("withdrawn invite started a game")
			}
		})
	}
}

func TestReplacedConnection(t *testing.T) {
	f := newHubFixture(t)
	old := f.login(userA)
	f.send(userA, ClientMsg{Type: TypeFind})

	fresh := f.login(userA)

	wantError(t, old, ErrCodeReplaced)

	if len(old.closed) != 1 || old.closed[0] != ErrCodeReplaced || len(fresh.closed) != 0 {
		t.Fatalf("closed: old %v, new %v", old.closed, fresh.closed)
	}

	f.hub.Disconnect(userA, old)
	f.hub.drain()

	f.send(userA, ClientMsg{Type: TypeFind})

	wantType(t, fresh, TypeQueued)

	cb := f.login(userB)
	f.send(userB, ClientMsg{Type: TypeFind})

	wantMatch(t, fresh, userB, true)
	wantMatch(t, cb, userA, false)
}

func TestReplacedConnectionKeepsRoom(t *testing.T) {
	f := newHubFixture(t)
	_, cb := f.play(userA, userB)

	fresh := f.login(userA)
	f.send(userA, ClientMsg{Type: TypeWord, Text: "москва"})

	if m := wantType(t, fresh, TypeWord); m["status"] != WordAccepted {
		t.Fatalf("new conn: %v", m)
	}

	wantType(t, cb, TypeWord)
}

func TestDisconnectInGame(t *testing.T) {
	f := newHubFixture(t)
	_, cb := f.play(userA, userB)
	f.send(userA, ClientMsg{Type: TypeWord, Text: "москва"})

	f.disconnect(userA)

	wantGameOver(t, cb, ReasonOpponentLeft, userA)

	if len(f.st.games) != 1 || f.st.games[0].StarterID != userA || f.st.games[0].InvitedID != userB ||
		f.st.games[0].Words != 1 {
		t.Fatalf("games: %+v", f.st.games)
	}

	f.send(userB, ClientMsg{Type: TypeFind})

	wantType(t, cb, TypeQueued)
}

func TestGameRecordedBeforeGameOver(t *testing.T) {
	f := newHubFixture(t)
	ca, cb := f.play(userA, userB)
	f.send(userA, ClientMsg{Type: TypeWord, Text: "москва"})
	f.send(userB, ClientMsg{Type: TypeWord, Text: "анапа"})

	recorded := map[string]int{}
	watch := func(name string, c *fakeConn) {
		c.onSend = func(msg []byte) {
			if toMap(t, msg)["type"] == TypeGameOver {
				f.st.mu.Lock()
				recorded[name] = len(f.st.games)
				f.st.mu.Unlock()
			}
		}
	}

	watch("a", ca)
	watch("b", cb)
	f.send(userA, ClientMsg{Type: TypeLeave})

	if recorded["a"] != 1 || recorded["b"] != 1 {
		t.Fatalf("games recorded when gameOver was sent: %v, want 1 for both", recorded)
	}
}

func TestGameWithoutWordsNotRecorded(t *testing.T) {
	f := newHubFixture(t)
	ca, cb := f.play(userA, userB)

	f.send(userB, ClientMsg{Type: TypeLeave})

	wantGameOver(t, ca, ReasonSurrender, userB)
	wantGameOver(t, cb, ReasonSurrender, userB)

	if len(f.st.games) != 0 {
		t.Fatalf("games: %+v", f.st.games)
	}

	f.send(userA, ClientMsg{Type: TypeFind})

	wantType(t, ca, TypeQueued)
}

func TestMoveTimeout(t *testing.T) {
	f := newHubFixture(t)
	ca, cb := f.play(userA, userB)
	f.send(userA, ClientMsg{Type: TypeWord, Text: "москва"})

	f.clk.Advance(moveTime)
	f.hub.drain()

	wantGameOver(t, ca, ReasonTimeout, userB)
	wantGameOver(t, cb, ReasonTimeout, userB)

	if len(f.st.games) != 1 || f.st.games[0].Duration != moveTime {
		t.Fatalf("games: %+v", f.st.games)
	}

	f.send(userB, ClientMsg{Type: TypeFind})

	wantType(t, cb, TypeQueued)
}

func TestChatRules(t *testing.T) {
	f := newHubFixture(t)
	ca, cb := f.play(userA, userB)

	f.send(userA, ClientMsg{Type: TypeChat, Text: strings.Repeat("я", 201)})

	wantError(t, ca, ErrCodeBadMessage)

	if cb.count(TypeChat) != 0 {
		t.Fatal("long chat delivered")
	}

	f.send(userA, ClientMsg{Type: TypeChat, Text: strings.Repeat("я", 200)})

	if m := wantType(t, cb, TypeChat); m["by"] != float64(userA) {
		t.Fatalf("chat: %v", m)
	}

	f.st.block(userB, userA)
	f.send(userA, ClientMsg{Type: TypeChat, Text: "привет"})

	if cb.count(TypeChat) != 1 {
		t.Fatal("chat delivered after block")
	}

	f.send(userA, ClientMsg{Type: TypeWord, Text: "москва"})

	if m := wantType(t, cb, TypeWord); m["status"] != WordAccepted {
		t.Fatalf("word after block: %v", m)
	}
}

// chanConn delivers messages to a channel so that a test can wait for a
// hub that runs in its own goroutine.
type chanConn struct{ ch chan []byte }

func (c *chanConn) Send(msg []byte) { c.ch <- msg }

func (c *chanConn) Close(string) {}

func TestHubRunProcessesEventsUntilCancelled(t *testing.T) {
	f := newHubFixture(t)
	ctx, cancel := context.WithCancel(context.Background())
	finished := make(chan struct{})

	go func() {
		f.hub.Run(ctx)
		close(finished)
	}()

	c := &chanConn{ch: make(chan []byte, 4)}
	f.hub.Connect(f.st.users[userA], c)
	f.hub.Message(userA, c, ClientMsg{Type: TypeHello, Protocol: 1, Client: "test"})

	select {
	case msg := <-c.ch:
		if got := toMap(t, msg); got["type"] != TypeWelcome {
			t.Fatalf("want welcome, got %v", got)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("hub did not process events")
	}

	cancel()

	select {
	case <-finished:
	case <-time.After(5 * time.Second):
		t.Fatal("Run did not exit after cancel")
	}

	f.hub.Message(userA, c, ClientMsg{Type: TypeFind}) // must not block after Run exited
}

func TestMessageFromReplacedConnectionIgnored(t *testing.T) {
	f := newHubFixture(t)
	u := f.st.users[userA]
	old, fresh := &fakeConn{}, &fakeConn{}

	f.hub.Connect(u, old)
	f.hub.Connect(u, fresh)
	f.hub.drain()

	f.hub.Message(userA, old, ClientMsg{Type: TypeHello, Protocol: 1, Client: "test"})
	f.hub.drain()

	if fresh.count(TypeWelcome) != 0 || old.count(TypeWelcome) != 0 {
		t.Fatalf("message from replaced conn was handled: fresh=%v old=%v", fresh.msgs, old.msgs)
	}

	f.hub.Message(userA, fresh, ClientMsg{Type: TypeHello, Protocol: 1, Client: "test"})
	f.hub.drain()

	if fresh.count(TypeWelcome) != 1 {
		t.Fatalf("fresh hello not handled: %v", fresh.msgs)
	}
}

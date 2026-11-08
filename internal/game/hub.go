package game

import (
	"context"
	"log"
	"slices"
	"sync"
	"time"
	"unicode/utf8"

	"lps/internal/dict"
	"lps/internal/fcm"
	"lps/internal/store"
)

const (
	protocolVersion = 1
	maxChatRunes    = 200
	callTimeout     = 3 * time.Second
	eventBuffer     = 256
)

// HubStore is the part of the store the hub needs.
type HubStore interface {
	User(ctx context.Context, id int64) (store.User, error)
	Relation(ctx context.Context, viewer, other int64) (store.Relation, error)
	RecordGame(ctx context.Context, g store.Game) (int64, error)
}

// Conn is a player's connection: a WebSocket transport or a bot.
type Conn interface {
	Send(msg []byte)
	Close(code string)
}

// HubConfig holds the game timings.
type HubConfig struct {
	MoveTime, InviteTTL, BotsWait time.Duration
	BotsEnabled                   bool
}

type sessionState int

const (
	stateConnected sessionState = iota // waiting for hello
	stateIdle
	stateQueued
	stateInviting
	statePlaying
)

// session is a connected user. It is also the user's Seat in a room.
type session struct {
	user  store.User
	conn  Conn
	state sessionState
	game  *activeGame

	queueSeq int64 // identifies the current queue entry for the bot wait
}

// UserID implements Seat.
func (s *session) UserID() int64 { return s.user.ID }

// Send implements Seat; it does nothing once the user has disconnected.
func (s *session) Send(msg []byte) {
	if s.conn != nil {
		s.conn.Send(msg)
	}
}

type activeGame struct {
	room    *Room
	players [2]*session
}

func (g *activeGame) opponent(s *session) *session {
	if g.players[0] == s {
		return g.players[1]
	}

	return g.players[0]
}

// Hub owns sessions, the matchmaking queue, invites and rooms. Its state is
// touched only by the Run goroutine: public methods and timers just enqueue
// events.
type Hub struct {
	cfg  HubConfig
	st   HubStore
	dict *dict.Dictionary
	push fcm.Sender
	clk  Clock

	events   chan func()
	done     chan struct{}
	ctx      context.Context
	handlers map[string]func(*session, ClientMsg)

	sessions map[int64]*session
	queue    []*session
	invites  map[int64]*invite
	rooms    map[int64]*activeGame
	lastRoom int64
	bots     []*session // in AddBot order
	queueSeq int64

	invitedAt    map[int64]time.Time // last invite per user, for the throttle
	invitesSwept time.Time

	pushes sync.WaitGroup // invite pushes in flight
}

// NewHub creates a hub. Call Run to start processing events.
func NewHub(cfg HubConfig, st HubStore, d *dict.Dictionary, push fcm.Sender, clk Clock) *Hub {
	h := &Hub{
		cfg:       cfg,
		st:        st,
		dict:      d,
		push:      push,
		clk:       clk,
		events:    make(chan func(), eventBuffer),
		done:      make(chan struct{}),
		ctx:       context.Background(),
		sessions:  make(map[int64]*session),
		invites:   make(map[int64]*invite),
		rooms:     make(map[int64]*activeGame),
		invitedAt: make(map[int64]time.Time),
	}
	h.handlers = map[string]func(*session, ClientMsg){
		TypeHello:       h.hello,
		TypeFind:        h.find,
		TypeInvite:      h.invite,
		TypeInviteReply: h.inviteReply,
		TypeCancel:      h.cancel,
		TypeWord:        h.word,
		TypeChat:        h.chat,
		TypeLeave:       h.leave,
	}

	return h
}

// Run processes events one at a time until ctx is done.
func (h *Hub) Run(ctx context.Context) {
	h.ctx = ctx

	defer close(h.done)

	for {
		select {
		case <-ctx.Done():
			return
		case e := <-h.events:
			e()
		}
	}
}

// Connect registers a connection for u. An existing connection of the same
// user is replaced: its game is lost, then it gets error{replaced} and is
// closed.
func (h *Hub) Connect(u store.User, c Conn) {
	h.enqueue(func() { h.connect(u, c) })
}

// Message handles a client message from userID received on c. It is
// ignored unless c is still the user's current connection.
func (h *Hub) Message(userID int64, c Conn, m ClientMsg) {
	h.enqueue(func() {
		if s := h.sessions[userID]; s == nil || s.conn != c {
			return
		}

		h.message(userID, m)
	})
}

// Disconnect drops the user's session unless c has already been replaced.
func (h *Hub) Disconnect(userID int64, c Conn) {
	h.enqueue(func() { h.disconnect(userID, c) })
}

// AddBot registers a bot as an idle player. With BotsEnabled the hub
// offers free bots to players who waited BotsWait in the queue.
func (h *Hub) AddBot(b *Bot) {
	h.enqueue(func() { h.addBot(b) })
}

func (h *Hub) enqueue(e func()) {
	select {
	case h.events <- e:
	case <-h.done:
	}
}

func (h *Hub) connect(u store.User, c Conn) {
	s, ok := h.sessions[u.ID]
	if !ok {
		h.sessions[u.ID] = &session{user: u, conn: c}

		return
	}

	h.forfeit(s)

	old := s.conn
	old.Send(Error(ErrCodeReplaced))
	old.Close(ErrCodeReplaced)

	h.toIdle(s)
	s.user, s.conn, s.state = u, c, stateConnected
}

func (h *Hub) disconnect(userID int64, c Conn) {
	s, ok := h.sessions[userID]
	if !ok || s.conn != c {
		return
	}

	h.drop(s)
}

// drop forgets a session: its queue place and invite go away and its game
// is lost.
func (h *Hub) drop(s *session) {
	s.conn = nil
	delete(h.sessions, s.user.ID)
	h.toIdle(s)
	h.forfeit(s)
}

// forfeit ends the game of s, if any, as if s had left: the opponent wins.
func (h *Hub) forfeit(s *session) {
	if g := s.game; g != nil {
		g.room.Leave(s.user.ID, ReasonOpponentLeft)
		h.finishGame(g)
	}
}

func (h *Hub) message(userID int64, m ClientMsg) {
	s, ok := h.sessions[userID]
	if !ok {
		return
	}

	if s.state == stateConnected && m.Type != TypeHello {
		s.Send(Error(ErrCodeHelloRequired))

		return
	}

	handle, ok := h.handlers[m.Type]
	if !ok {
		s.Send(Error(ErrCodeBadMessage))

		return
	}

	handle(s, m)
}

func (h *Hub) hello(s *session, m ClientMsg) {
	if s.state != stateConnected {
		s.Send(Error(ErrCodeInvalidState))

		return
	}

	if m.Protocol != protocolVersion {
		s.Send(Error(ErrCodeUnsupportedProtocol))
		s.conn.Close(ErrCodeUnsupportedProtocol)
		h.drop(s)

		return
	}

	s.state = stateIdle
	s.Send(Welcome(userView(s.user)))
}

// find queues an idle player. An inviter may search too: the invite is
// withdrawn because the inviter starts another game.
func (h *Hub) find(s *session, _ ClientMsg) {
	if s.state != stateIdle && s.state != stateInviting {
		s.Send(Error(ErrCodeInvalidState))

		return
	}

	h.withdraw(s.user.ID)
	s.state = stateQueued
	s.Send(Queued())

	h.matchQueued(s)
}

// matchQueued pairs s with the earliest queued player it is not blocked
// with, or puts s at the end of the queue.
func (h *Hub) matchQueued(s *session) {
	for _, q := range h.queue {
		ab, ba, err := h.relations(q, s)
		if err != nil {
			s.state = stateIdle
			h.fail(s, "match", err)

			return
		}

		if ab.Blocked == store.BlockedNone {
			h.dequeue(q)
			h.startGame(q, s, ab, ba)

			return
		}
	}

	h.joinQueue(s)
}

// joinQueue puts s at the end of the queue and, with bots enabled, arms the
// bot wait for this queue entry.
func (h *Hub) joinQueue(s *session) {
	h.queue = append(h.queue, s)

	if !h.cfg.BotsEnabled {
		return
	}

	h.queueSeq++
	s.queueSeq = h.queueSeq
	h.armBotWait(s.user.ID, s.queueSeq, h.cfg.BotsWait)
}

func (h *Hub) cancel(s *session, _ ClientMsg) {
	switch s.state {
	case stateQueued, stateInviting:
		h.toIdle(s)
	default:
		s.Send(Error(ErrCodeInvalidState))
	}
}

func (h *Hub) word(s *session, m ClientMsg) {
	if s.state != statePlaying {
		s.Send(Error(ErrCodeInvalidState))

		return
	}

	s.game.room.Word(s.user.ID, m.Text)
}

func (h *Hub) chat(s *session, m ClientMsg) {
	if s.state != statePlaying {
		s.Send(Error(ErrCodeInvalidState))

		return
	}

	if utf8.RuneCountInString(m.Text) > maxChatRunes {
		s.Send(Error(ErrCodeBadMessage))

		return
	}

	rel, err := h.relation(s.user.ID, s.game.opponent(s).user.ID)
	if err != nil {
		h.fail(s, "chat", err)

		return
	}

	s.game.room.Chat(s.user.ID, m.Text, rel.Blocked != store.BlockedNone)
}

func (h *Hub) leave(s *session, _ ClientMsg) {
	if s.state != statePlaying {
		s.Send(Error(ErrCodeInvalidState))

		return
	}

	g := s.game
	g.room.Leave(s.user.ID, ReasonSurrender)
	h.finishGame(g)
}

// startGame creates a room where a moves first. ab is a's relation to b,
// ba the other way round.
func (h *Hub) startGame(a, b *session, ab, ba store.Relation) {
	h.lastRoom++
	g := &activeGame{players: [2]*session{a, b}}
	g.room = NewRoom(h.lastRoom, a, b, opponent(a.user, ba), opponent(b.user, ab),
		h.dict, h.cfg.MoveTime, h.clk, h.onRoomTimer)
	g.room.OnFinish = h.recordGame
	h.rooms[g.room.ID] = g

	for _, p := range g.players {
		p.game, p.state = g, statePlaying
	}

	g.room.Start()
}

func (h *Hub) onRoomTimer(roomID int64, move int) {
	h.enqueue(func() { h.roomTimer(roomID, move) })
}

func (h *Hub) roomTimer(roomID int64, move int) {
	g, ok := h.rooms[roomID]
	if ok && g.room.Timer(move) {
		h.finishGame(g)
	}
}

// finishGame is called after the room has sent gameOver: it frees both
// players. The game is already recorded by recordGame.
func (h *Hub) finishGame(g *activeGame) {
	delete(h.rooms, g.room.ID)

	for _, p := range g.players {
		p.game = nil

		if p.state == statePlaying {
			p.state = stateIdle
		}
	}
}

// recordGame is the room's finish hook: it runs before gameOver is sent, so
// a client that has seen gameOver finds the game in its history.
func (h *Hub) recordGame(res store.Game, record bool) {
	if !record {
		return
	}

	ctx, cancel := h.callCtx()
	defer cancel()

	if _, err := h.st.RecordGame(ctx, res); err != nil {
		log.Printf("game: record game %d-%d: %v", res.StarterID, res.InvitedID, err)
	}
}

// toIdle takes s out of the queue and withdraws its invite.
func (h *Hub) toIdle(s *session) {
	h.dequeue(s)
	h.withdraw(s.user.ID)
}

func (h *Hub) dequeue(s *session) {
	i := slices.Index(h.queue, s)
	if i < 0 {
		return
	}

	h.queue = slices.Delete(h.queue, i, i+1)
	s.state = stateIdle
}

// fail logs a store error and reports it to the requester.
func (h *Hub) fail(s *session, op string, err error) {
	log.Printf("game: %s for user %d: %v", op, s.user.ID, err)
	s.Send(Error(ErrCodeInvalidState))
}

// callCtx bounds one store call.
func (h *Hub) callCtx() (context.Context, context.CancelFunc) {
	return context.WithTimeout(h.ctx, callTimeout)
}

func (h *Hub) relation(viewer, other int64) (store.Relation, error) {
	ctx, cancel := h.callCtx()
	defer cancel()

	return h.st.Relation(ctx, viewer, other)
}

func (h *Hub) relations(a, b *session) (ab, ba store.Relation, err error) {
	if ab, err = h.relation(a.user.ID, b.user.ID); err != nil {
		return ab, ba, err
	}

	ba, err = h.relation(b.user.ID, a.user.ID)

	return ab, ba, err
}

func (h *Hub) user(id int64) (store.User, error) {
	ctx, cancel := h.callCtx()
	defer cancel()

	return h.st.User(ctx, id)
}

func userView(u store.User) UserView {
	v := UserView{ID: u.ID, Name: u.Name, AvatarHash: optional(u.AvatarHash), Provider: u.Provider}
	if !u.LastSeenAt.IsZero() {
		v.LastSeenAt = u.LastSeenAt.UnixMilli()
	}

	return v
}

func opponent(u store.User, rel store.Relation) Opponent {
	return Opponent{
		ID:         u.ID,
		Name:       u.Name,
		AvatarHash: optional(u.AvatarHash),
		Friendship: string(rel.Friendship),
	}
}

func optional(s string) *string {
	if s == "" {
		return nil
	}

	return &s
}

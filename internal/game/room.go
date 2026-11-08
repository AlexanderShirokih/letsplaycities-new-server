package game

import (
	"time"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/dict"
	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

// Seat is one player's connection as seen by a room. Send to a
// disconnected seat must be a no-op.
type Seat interface {
	UserID() int64
	Send(msg []byte)
}

// Room is one match. It is not safe for concurrent use: only the hub calls
// it, from a single goroutine. The move timer never touches the room, it
// only calls onTimer, after which the hub calls Timer.
type Room struct {
	ID int64

	// OnFinish, if set, is called once when the game ends, before gameOver
	// is sent, with the value of Result.
	OnFinish func(g store.Game, record bool)

	seats    [2]Seat // 0 is the starter, 1 is the invited player
	seenBy   [2]Opponent
	dict     *dict.Dictionary
	moveTime time.Duration
	clk      Clock
	onTimer  func(roomID int64, move int)

	used      map[string]struct{}
	last      string
	turn      int
	move      int
	words     int
	startedAt time.Time
	stopTimer func() bool
	finished  bool
	duration  time.Duration
}

// NewRoom creates a room. starterSeenByInvited is the Opponent sent to the
// invited player (it describes the starter); invitedSeenByStarter is the
// other way round.
func NewRoom(id int64, starter, invited Seat, starterSeenByInvited, invitedSeenByStarter Opponent,
	d *dict.Dictionary, moveTime time.Duration, clk Clock, onTimer func(roomID int64, move int),
) *Room {
	return &Room{
		ID:       id,
		seats:    [2]Seat{starter, invited},
		seenBy:   [2]Opponent{invitedSeenByStarter, starterSeenByInvited},
		dict:     d,
		moveTime: moveTime,
		clk:      clk,
		onTimer:  onTimer,
		used:     make(map[string]struct{}),
	}
}

// Start announces the match to both players and arms the move timer.
func (r *Room) Start() {
	r.startedAt = r.clk.Now()
	sec := int(r.moveTime / time.Second)

	r.seats[0].Send(Match(r.ID, r.seenBy[0], true, sec))
	r.seats[1].Send(Match(r.ID, r.seenBy[1], false, sec))

	r.armTimer()
}

// Word handles a move. A finished room ignores it.
func (r *Room) Word(from int64, raw string) {
	if r.finished {
		return
	}

	idx := r.seatIndex(from)
	if idx < 0 {
		return
	}

	word := Normalize(raw)

	if idx != r.turn {
		r.reject(idx, word, WordNotYourTurn)

		return
	}

	if status := r.check(word); status != WordAccepted {
		r.reject(idx, word, status)

		return
	}

	r.accept(idx, word)
}

func (r *Room) check(word string) string {
	if word == "" {
		return WordUnknown
	}

	if r.last != "" && []rune(word)[0] != LastSuitableRune(r.last) {
		return WordWrongLetter
	}

	if _, dup := r.used[word]; dup {
		return WordAlreadyUsed
	}

	if !r.dict.Contains(word) {
		return WordUnknown
	}

	return WordAccepted
}

func (r *Room) reject(idx int, word, status string) {
	r.seats[idx].Send(Word(r.seats[idx].UserID(), word, status))
}

func (r *Room) accept(idx int, word string) {
	r.used[word] = struct{}{}
	r.last = word
	r.words++
	r.move++
	r.turn = 1 - idx

	msg := Word(r.seats[idx].UserID(), word, WordAccepted)
	r.seats[0].Send(msg)
	r.seats[1].Send(msg)

	r.armTimer()
}

// Chat relays a chat message to the opponent unless the players block each
// other.
func (r *Room) Chat(from int64, text string, blocked bool) {
	idx := r.seatIndex(from)
	if r.finished || blocked || idx < 0 {
		return
	}

	r.seats[1-idx].Send(Chat(from, text))
}

// Timer ends the game by timeout if move is still the current move.
func (r *Room) Timer(move int) (finished bool) {
	if r.finished || move != r.move {
		return false
	}

	r.finish(ReasonTimeout, r.seats[r.turn].UserID())

	return true
}

// Leave ends the game; the player who leaves loses. reason is
// ReasonSurrender or ReasonOpponentLeft.
func (r *Room) Leave(from int64, reason string) {
	if r.finished || r.seatIndex(from) < 0 {
		return
	}

	r.finish(reason, from)
}

// Result returns the game for the history. The flag is false when no word
// was accepted and the game must not be recorded.
func (r *Room) Result() (store.Game, bool) {
	d := r.duration
	if !r.finished {
		d = r.clk.Now().Sub(r.startedAt)
	}

	return store.Game{
		ID:        r.ID,
		StarterID: r.seats[0].UserID(),
		InvitedID: r.seats[1].UserID(),
		StartedAt: r.startedAt,
		Duration:  d,
		Words:     r.words,
	}, r.words > 0
}

func (r *Room) finish(reason string, loser int64) {
	r.finished = true
	r.duration = r.clk.Now().Sub(r.startedAt)

	if r.stopTimer != nil {
		r.stopTimer()
	}

	if r.OnFinish != nil {
		r.OnFinish(r.Result())
	}

	msg := GameOver(reason, loser, r.words, int(r.duration/time.Second))
	r.seats[0].Send(msg)
	r.seats[1].Send(msg)
}

func (r *Room) armTimer() {
	if r.stopTimer != nil {
		r.stopTimer()
	}

	move := r.move
	r.stopTimer = r.clk.AfterFunc(r.moveTime, func() { r.onTimer(r.ID, move) })
}

func (r *Room) seatIndex(id int64) int {
	switch id {
	case r.seats[0].UserID():
		return 0
	case r.seats[1].UserID():
		return 1
	}

	return -1
}

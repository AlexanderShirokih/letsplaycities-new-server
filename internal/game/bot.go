package game

import (
	"encoding/json"
	"errors"
	"fmt"
	"math/rand"
	"os"
	"time"

	"lps/internal/dict"
)

// BotSpec describes one bot in the bots file. Level is the highest city
// difficulty the bot plays; it answers MinDelaySec to MaxDelaySec seconds
// after the opponent's move.
type BotSpec struct {
	UserID        int64    `json:"userId"`
	Level         uint8    `json:"level"`
	PrefCountries []uint16 `json:"prefCountries"`
	MinDelaySec   int      `json:"minDelaySec"`
	MaxDelaySec   int      `json:"maxDelaySec"`
}

var errBadBotSpec = errors.New("bad bot spec")

// LoadBots reads a JSON array of BotSpec.
func LoadBots(path string) ([]BotSpec, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("game: load bots: %w", err)
	}

	var specs []BotSpec

	if err := json.Unmarshal(b, &specs); err != nil {
		return nil, fmt.Errorf("game: load bots %s: %w", path, err)
	}

	for _, s := range specs {
		if s.UserID <= 0 || s.MinDelaySec < 0 || s.MaxDelaySec < s.MinDelaySec {
			return nil, fmt.Errorf("game: load bots %s: %w: %+v", path, errBadBotSpec, s)
		}
	}

	return specs, nil
}

// Bot is a computer player. It implements Conn: the hub delivers server
// messages to Send from its own goroutine, and the bot answers by
// scheduling a hub event on the clock, so Send never blocks the hub.
type Bot struct {
	spec BotSpec
	hub  *Hub
	dict *dict.Dictionary
	clk  Clock
	rnd  *rand.Rand

	used     map[string]struct{}
	stopMove func() bool
	gen      uint64 // incremented when the game state resets; hub goroutine only
}

// botMsg holds the fields of the server messages a bot reacts to.
type botMsg struct {
	Type   string `json:"type"`
	By     int64  `json:"by"`
	Text   string `json:"text"`
	Status string `json:"status"`
}

// NewBot creates a bot. Register it with Hub.AddBot. rnd is used only from
// the hub goroutine.
func NewBot(spec BotSpec, h *Hub, d *dict.Dictionary, clk Clock, rnd *rand.Rand) *Bot {
	return &Bot{spec: spec, hub: h, dict: d, clk: clk, rnd: rnd, used: make(map[string]struct{})}
}

// Send implements Conn. It reacts to the start and end of a game and to
// accepted words; everything else is ignored.
func (b *Bot) Send(msg []byte) {
	var m botMsg

	if err := json.Unmarshal(msg, &m); err != nil {
		return
	}

	switch m.Type {
	case TypeMatch, TypeGameOver:
		b.reset()
	case TypeWord:
		if m.Status == WordAccepted {
			b.accepted(m.By, m.Text)
		}
	}
}

// Close implements Conn. A bot has no transport to close.
func (b *Bot) Close(string) {}

// reset forgets the current game and cancels a pending move.
func (b *Bot) reset() {
	if b.stopMove != nil {
		b.stopMove()
		b.stopMove = nil
	}

	b.used = make(map[string]struct{})
	b.gen++
}

// accepted records a word of either player and answers the opponent's.
func (b *Bot) accepted(by int64, word string) {
	b.used[word] = struct{}{}

	if by == b.spec.UserID {
		return
	}

	used := func(w string) bool {
		_, ok := b.used[w]

		return ok
	}

	answer, ok := b.dict.Pick(LastSuitableRune(word), b.spec.Level, b.spec.PrefCountries, used, b.rnd)
	if !ok {
		return
	}

	b.stopMove = b.clk.AfterFunc(b.delay(), b.moveTimer(b.gen, answer))
}

// moveTimer returns the timer callback for a move planned in generation
// gen. It only enqueues a hub event; the event delivers the word unless
// the game state has been reset since, which stop cannot prevent once the
// timer has fired.
func (b *Bot) moveTimer(gen uint64, answer string) func() {
	h, id := b.hub, b.spec.UserID

	return func() {
		h.enqueue(func() {
			if b.gen == gen {
				h.message(id, ClientMsg{Type: TypeWord, Text: answer})
			}
		})
	}
}

// delay returns a random answer delay in [MinDelaySec, MaxDelaySec].
func (b *Bot) delay() time.Duration {
	sec := b.spec.MinDelaySec
	if span := b.spec.MaxDelaySec - sec; span > 0 {
		sec += b.rnd.Intn(span + 1)
	}

	return time.Duration(sec) * time.Second
}

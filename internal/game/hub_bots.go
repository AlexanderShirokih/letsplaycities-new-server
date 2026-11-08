package game

import (
	"log"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

func (h *Hub) addBot(b *Bot) {
	id := b.spec.UserID
	if _, ok := h.sessions[id]; ok {
		log.Printf("game: add bot %d: user is already connected", id)

		return
	}

	u, err := h.user(id)
	if err != nil {
		log.Printf("game: add bot %d: %v", id, err)

		return
	}

	s := &session{user: u, conn: b, state: stateIdle}
	h.sessions[id] = s
	h.bots = append(h.bots, s)
}

// armBotWait schedules a bot check for the queue entry seq of userID.
func (h *Hub) armBotWait(userID, seq int64) {
	h.clk.AfterFunc(h.cfg.BotsWait, func() {
		h.enqueue(func() { h.botCheck(userID, seq) })
	})
}

// botCheck matches a player who is still in the same queue entry with the
// first free bot it is not blocked with. The player moves first. Without a
// free bot the player stays in the queue and the bot wait is armed again
// for the same entry.
func (h *Hub) botCheck(userID, seq int64) {
	s, ok := h.sessions[userID]
	if !ok || s.state != stateQueued || s.queueSeq != seq {
		return
	}

	bot, ab, ba, err := h.freeBot(s)

	switch {
	case err != nil:
		log.Printf("game: bot match for user %d: %v", userID, err)
	case bot == nil:
		h.armBotWait(userID, seq)
	default:
		h.dequeue(s)
		h.startGame(s, bot, ab, ba)
	}
}

// freeBot returns the first idle bot that s is not blocked with, with the
// relations s to bot and bot to s. The bot is nil when there is none.
func (h *Hub) freeBot(s *session) (bot *session, ab, ba store.Relation, err error) {
	for _, b := range h.bots {
		if b.game != nil || b.state != stateIdle {
			continue
		}

		if ab, ba, err = h.relations(s, b); err != nil {
			return nil, ab, ba, err
		}

		if ab.Blocked == store.BlockedNone {
			return b, ab, ba, nil
		}
	}

	return nil, ab, ba, nil
}

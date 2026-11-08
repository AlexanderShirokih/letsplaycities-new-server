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

// botCheck matches a player who is still in the same queue entry with the
// first free bot it is not blocked with. The player moves first. Without a
// free bot the player stays in the queue.
func (h *Hub) botCheck(userID, seq int64) {
	s, ok := h.sessions[userID]
	if !ok || s.state != stateQueued || s.queueSeq != seq {
		return
	}

	for _, bot := range h.bots {
		if bot.game != nil || bot.state != stateIdle {
			continue
		}

		ab, ba, err := h.relations(s, bot)
		if err != nil {
			log.Printf("game: bot match for user %d: %v", userID, err)

			return
		}

		if ab.Blocked == store.BlockedNone {
			h.dequeue(s)
			h.startGame(s, bot, ab, ba)

			return
		}
	}
}

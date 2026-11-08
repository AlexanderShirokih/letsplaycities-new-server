package game

import (
	"log"
	"strconv"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

// invite is an outstanding invitation, keyed by the inviter.
type invite struct {
	from, to int64
	stop     func() bool
}

func (h *Hub) invite(s *session, m ClientMsg) {
	if s.state != stateIdle {
		s.Send(Error(ErrCodeInvalidState))

		return
	}

	status, err := h.inviteStatus(s.user, m.UserID)
	if err != nil {
		h.fail(s, "invite", err)

		return
	}

	if status == InviteSent {
		h.addInvite(s, m.UserID)
	}

	s.Send(InviteStatus(m.UserID, status))
}

// inviteStatus runs the invite checks in the order of the spec and sends
// the push when they pass.
func (h *Hub) inviteStatus(from store.User, to int64) (string, error) {
	rel, err := h.relation(from.ID, to)
	if err != nil {
		return "", err
	}

	switch {
	case rel.Blocked != store.BlockedNone:
		return InviteBlocked, nil
	case rel.Friendship != store.FriendFriends:
		return InviteNotFriends, nil
	case h.playing(to):
		return InviteBusy, nil
	}

	target, err := h.user(to)
	if err != nil {
		return "", err
	}

	if target.FCMToken == "" || !h.sendInvitePush(from, target.FCMToken) {
		return InviteOffline, nil
	}

	return InviteSent, nil
}

func (h *Hub) playing(userID int64) bool {
	s, ok := h.sessions[userID]

	return ok && s.game != nil
}

func (h *Hub) sendInvitePush(from store.User, token string) bool {
	ctx, cancel := h.callCtx()
	defer cancel()

	data := map[string]string{
		"type":     "invite",
		"fromId":   strconv.FormatInt(from.ID, 10),
		"fromName": from.Name,
	}

	if err := h.push.Send(ctx, token, data); err != nil {
		log.Printf("game: invite push from %d: %v", from.ID, err)

		return false
	}

	return true
}

func (h *Hub) addInvite(s *session, to int64) {
	inv := &invite{from: s.user.ID, to: to}
	inv.stop = h.clk.AfterFunc(h.cfg.InviteTTL, func() {
		h.enqueue(func() { h.inviteExpired(inv) })
	})

	h.invites[inv.from] = inv
	s.state = stateInviting
}

func (h *Hub) inviteExpired(inv *invite) {
	if h.invites[inv.from] != inv {
		return
	}

	h.withdraw(inv.from)
	h.sessions[inv.from].Send(InviteStatus(inv.to, InviteExpired))
}

func (h *Hub) inviteReply(s *session, m ClientMsg) {
	inv, ok := h.invites[m.UserID]
	if !ok || inv.to != s.user.ID {
		s.Send(InviteStatus(m.UserID, InviteExpired))

		return
	}

	if !m.Accept {
		h.withdraw(inv.from)
		h.sessions[inv.from].Send(InviteStatus(s.user.ID, InviteDeclined))

		return
	}

	h.acceptInvite(s, inv)
}

func (h *Hub) acceptInvite(s *session, inv *invite) {
	if s.state == statePlaying {
		s.Send(Error(ErrCodeInvalidState))

		return
	}

	inviter := h.sessions[inv.from]

	ab, ba, err := h.relations(inviter, s)
	if err != nil {
		h.fail(s, "accept", err)

		return
	}

	h.withdraw(inv.from)

	if ab.Blocked != store.BlockedNone {
		s.Send(InviteStatus(inviter.user.ID, InviteBlocked))
		inviter.Send(InviteStatus(s.user.ID, InviteBlocked))

		return
	}

	h.toIdle(s)
	h.startGame(inviter, s, ab, ba)
}

func (h *Hub) withdraw(from int64) {
	inv, ok := h.invites[from]
	if !ok {
		return
	}

	inv.stop()
	delete(h.invites, from)

	if s, ok := h.sessions[from]; ok && s.state == stateInviting {
		s.state = stateIdle
	}
}

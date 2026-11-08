package game

import (
	"context"
	"log"
	"strconv"
	"time"

	"lps/internal/store"
)

const (
	// inviteInterval is the shortest time between two invites of one user.
	inviteInterval = 2 * time.Second

	// pushTimeout bounds one invite push, which runs off the hub goroutine.
	pushTimeout = 10 * time.Second
)

// invite is an outstanding invitation, keyed by the inviter. It is
// registered before its push is sent, so the invitee may answer a push that
// is still being confirmed.
type invite struct {
	from, to int64
	stop     func() bool
}

func (h *Hub) invite(s *session, m ClientMsg) {
	if s.state != stateIdle {
		s.Send(Error(ErrCodeInvalidState))

		return
	}

	if !h.allowInvite(s.user.ID) {
		s.Send(Error(ErrCodeRateLimited))

		return
	}

	token, status, err := h.inviteTarget(s.user.ID, m.UserID)
	if err != nil {
		h.fail(s, "invite", err)

		return
	}

	if status != "" {
		s.Send(InviteStatus(m.UserID, status))

		return
	}

	inv := h.addInvite(s, m.UserID)
	h.sendInvitePush(inv, s.user, token)
}

// allowInvite lets a user invite at most once per inviteInterval. Users
// stay in invitedAt across reconnects until their entry is swept.
func (h *Hub) allowInvite(userID int64) bool {
	now := h.clk.Now()
	h.sweepInviteTimes(now)

	if last, ok := h.invitedAt[userID]; ok && now.Sub(last) < inviteInterval {
		return false
	}

	h.invitedAt[userID] = now

	return true
}

// sweepInviteTimes forgets stale invite times at most once per interval,
// so invitedAt holds only the users who invited recently.
func (h *Hub) sweepInviteTimes(now time.Time) {
	if now.Sub(h.invitesSwept) < inviteInterval {
		return
	}

	for id, at := range h.invitedAt {
		if now.Sub(at) >= inviteInterval {
			delete(h.invitedAt, id)
		}
	}

	h.invitesSwept = now
}

// inviteTarget runs the invite checks in the order of the spec. It returns
// the invitee's FCM token when the push is to be sent, or the final status.
func (h *Hub) inviteTarget(from, to int64) (token, status string, err error) {
	rel, err := h.relation(from, to)
	if err != nil {
		return "", "", err
	}

	switch {
	case rel.Blocked != store.BlockedNone:
		return "", InviteBlocked, nil
	case rel.Friendship != store.FriendFriends:
		return "", InviteNotFriends, nil
	case h.playing(to):
		return "", InviteBusy, nil
	}

	target, err := h.user(to)
	if err != nil {
		return "", "", err
	}

	if target.FCMToken == "" {
		return "", InviteOffline, nil
	}

	return target.FCMToken, "", nil
}

func (h *Hub) playing(userID int64) bool {
	s, ok := h.sessions[userID]

	return ok && s.game != nil
}

// sendInvitePush sends the push in its own goroutine and reports the
// result to the hub as an event.
func (h *Hub) sendInvitePush(inv *invite, from store.User, token string) {
	ctx, cancel := context.WithTimeout(h.ctx, pushTimeout)
	data := map[string]string{
		"type":     "invite",
		"fromId":   strconv.FormatInt(from.ID, 10),
		"fromName": from.Name,
	}

	h.pushes.Add(1)

	go func() {
		defer h.pushes.Done()
		defer cancel()

		err := h.push.Send(ctx, token, data)
		if err != nil {
			log.Printf("game: invite push from %d: %v", from.ID, err)
		}

		h.enqueue(func() { h.invitePushed(inv, err == nil) })
	}()
}

// invitePushed answers the inviter once the push is done, unless the
// invite was withdrawn, answered or expired meanwhile.
func (h *Hub) invitePushed(inv *invite, delivered bool) {
	if h.invites[inv.from] != inv {
		return
	}

	if !delivered {
		h.withdraw(inv.from)
		h.sessions[inv.from].Send(InviteStatus(inv.to, InviteOffline))

		return
	}

	h.sessions[inv.from].Send(InviteStatus(inv.to, InviteSent))
}

func (h *Hub) addInvite(s *session, to int64) *invite {
	inv := &invite{from: s.user.ID, to: to}
	inv.stop = h.clk.AfterFunc(h.cfg.InviteTTL, func() {
		h.enqueue(func() { h.inviteExpired(inv) })
	})

	h.invites[inv.from] = inv
	s.state = stateInviting

	return inv
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

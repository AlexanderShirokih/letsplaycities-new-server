package api

import (
	"errors"
	"net/http"
	"strconv"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

const historyPage = 50

// peerID parses the {id} path value and checks that the user exists.
// On failure it has already written the response.
func (s *Server) peerID(w http.ResponseWriter, r *http.Request) (int64, bool) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil {
		writeError(w, http.StatusNotFound, "not_found", "Пользователь не найден")

		return 0, false
	}

	if id == userFrom(r).ID {
		writeError(w, http.StatusBadRequest, "self", "Нельзя выбрать себя")

		return 0, false
	}

	if _, err := s.d.Store.User(r.Context(), id); err != nil {
		if errors.Is(err, store.ErrNotFound) {
			writeError(w, http.StatusNotFound, "not_found", "Пользователь не найден")
		} else {
			writeInternal(w, err)
		}

		return 0, false
	}

	return id, true
}

func (s *Server) handleListFriends(w http.ResponseWriter, r *http.Request) {
	list, err := s.d.Store.Friends(r.Context(), userFrom(r).ID)
	if err != nil {
		writeInternal(w, err)

		return
	}

	out := make([]map[string]any, 0, len(list))
	for _, f := range list {
		out = append(out, map[string]any{"user": toUserJSON(f.User), "status": f.Status})
	}

	writeJSON(w, http.StatusOK, out)
}

func (s *Server) handlePutFriend(w http.ResponseWriter, r *http.Request) {
	id, ok := s.peerID(w, r)
	if !ok {
		return
	}

	ctx := r.Context()
	me := userFrom(r).ID

	rel, err := s.d.Store.Relation(ctx, me, id)
	if err != nil {
		writeInternal(w, err)

		return
	}

	if rel.Blocked != store.BlockedNone {
		writeError(w, http.StatusForbidden, "blocked", "Пользователь заблокирован")

		return
	}

	if err := s.d.Store.RequestOrAcceptFriend(ctx, me, id, s.d.Now()); err != nil {
		writeInternal(w, err)

		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (s *Server) handleDeleteFriend(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil {
		writeError(w, http.StatusNotFound, "not_found", "Пользователь не найден")

		return
	}

	if err := s.d.Store.RemoveFriendship(r.Context(), userFrom(r).ID, id); err != nil {
		writeInternal(w, err)

		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (s *Server) handleListBlocks(w http.ResponseWriter, r *http.Request) {
	list, err := s.d.Store.Blocks(r.Context(), userFrom(r).ID)
	if err != nil {
		writeInternal(w, err)

		return
	}

	out := make([]userJSON, 0, len(list))
	for _, u := range list {
		out = append(out, toUserJSON(u))
	}

	writeJSON(w, http.StatusOK, out)
}

func (s *Server) handlePutBlock(w http.ResponseWriter, r *http.Request) {
	id, ok := s.peerID(w, r)
	if !ok {
		return
	}

	if err := s.d.Store.Block(r.Context(), userFrom(r).ID, id); err != nil {
		writeInternal(w, err)

		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (s *Server) handleDeleteBlock(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil {
		writeError(w, http.StatusNotFound, "not_found", "Пользователь не найден")

		return
	}

	if err := s.d.Store.Unblock(r.Context(), userFrom(r).ID, id); err != nil {
		writeInternal(w, err)

		return
	}

	w.WriteHeader(http.StatusNoContent)
}

type historyGameJSON struct {
	ID          int64    `json:"id"`
	Opponent    userJSON `json:"opponent"`
	IsFriend    bool     `json:"isFriend"`
	StartedAt   int64    `json:"startedAt"`
	DurationSec int64    `json:"durationSec"`
	Words       int      `json:"words"`
}

// queryInt64 reads an optional non-negative integer query parameter.
func queryInt64(r *http.Request, name string) (int64, bool) {
	v := r.URL.Query().Get(name)
	if v == "" {
		return 0, true
	}

	n, err := strconv.ParseInt(v, 10, 64)

	return n, err == nil && n >= 0
}

func (s *Server) handleHistory(w http.ResponseWriter, r *http.Request) {
	with, ok1 := queryInt64(r, "with")
	before, ok2 := queryInt64(r, "before")

	if !ok1 || !ok2 {
		writeError(w, http.StatusBadRequest, "bad_request", "Некорректный запрос")

		return
	}

	entries, next, err := s.d.Store.History(r.Context(), userFrom(r).ID, with, before, historyPage)
	if err != nil {
		writeInternal(w, err)

		return
	}

	games := make([]historyGameJSON, 0, len(entries))
	for _, e := range entries {
		games = append(games, historyGameJSON{
			ID:          e.Game.ID,
			Opponent:    toUserJSON(e.Opponent),
			IsFriend:    e.IsFriend,
			StartedAt:   e.Game.StartedAt.UnixMilli(),
			DurationSec: int64(e.Game.Duration.Seconds()),
			Words:       e.Game.Words,
		})
	}

	var cursor *int64
	if next != 0 {
		cursor = &next
	}

	writeJSON(w, http.StatusOK, map[string]any{"games": games, "next": cursor})
}

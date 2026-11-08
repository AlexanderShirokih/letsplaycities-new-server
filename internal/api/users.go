package api

import (
	"errors"
	"net/http"
	"strconv"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

func (s *Server) handleGetUser(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil {
		writeError(w, http.StatusNotFound, "not_found", "Пользователь не найден")

		return
	}

	ctx := r.Context()
	u, err := s.d.Store.User(ctx, id)
	if errors.Is(err, store.ErrNotFound) {
		writeError(w, http.StatusNotFound, "not_found", "Пользователь не найден")

		return
	}

	if err != nil {
		writeInternal(w, err)

		return
	}

	rel, err := s.d.Store.Relation(ctx, userFrom(r).ID, id)
	if err != nil {
		writeInternal(w, err)

		return
	}

	writeJSON(w, http.StatusOK, struct {
		userJSON
		Friendship store.Friendship `json:"friendship"`
		Blocked    store.Blocked    `json:"blocked"`
	}{toUserJSON(u), rel.Friendship, rel.Blocked})
}

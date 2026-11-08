package api

import (
	"errors"
	"net/http"

	"lps/internal/store"
)

func (s *Server) handleGetUser(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, userNotFound)
	if !ok {
		return
	}

	ctx := r.Context()

	u, err := s.d.Store.User(ctx, id)
	if errors.Is(err, store.ErrNotFound) {
		writeNotFound(w, userNotFound)

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

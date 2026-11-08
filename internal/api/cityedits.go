package api

import (
	"net/http"
	"unicode/utf8"

	"lps/internal/store"
)

const (
	maxReasonRunes   = 64
	maxCityNameRunes = 100
	maxNewCityEdits  = 50
	cityEditsPerPage = 50
)

type cityEditJSON struct {
	ID             int64   `json:"id"`
	OldCountryCode int     `json:"oldCountryCode"`
	NewCountryCode int     `json:"newCountryCode"`
	OldName        *string `json:"oldName"`
	NewName        *string `json:"newName"`
	Reason         *string `json:"reason"`
	Verdict        *string `json:"verdict"`
	Status         string  `json:"status"`
}

func toCityEditJSON(e store.CityEdit) cityEditJSON {
	return cityEditJSON{
		ID: e.ID, OldCountryCode: e.OldCountry, NewCountryCode: e.NewCountry,
		OldName: e.OldName, NewName: e.NewName, Reason: e.Reason, Verdict: e.Verdict, Status: e.Status,
	}
}

func (s *Server) handleListCityEdits(w http.ResponseWriter, r *http.Request) {
	list, err := s.d.Store.CityEdits(r.Context(), userFrom(r).ID, cityEditsPerPage)
	if err != nil {
		writeInternal(w, err)

		return
	}

	out := make([]cityEditJSON, 0, len(list))
	for _, e := range list {
		out = append(out, toCityEditJSON(e))
	}

	writeJSON(w, http.StatusOK, out)
}

type cityEditRequest struct {
	OldCountryCode int     `json:"oldCountryCode"`
	NewCountryCode int     `json:"newCountryCode"`
	OldName        *string `json:"oldName"`
	NewName        *string `json:"newName"`
	Reason         *string `json:"reason"`
}

// longer reports whether s is set and longer than n runes.
func longer(s *string, n int) bool {
	return s != nil && utf8.RuneCountInString(*s) > n
}

// checkCityEdit validates the field lengths; on failure it answers 400.
func checkCityEdit(w http.ResponseWriter, req cityEditRequest) bool {
	if longer(req.OldName, maxCityNameRunes) || longer(req.NewName, maxCityNameRunes) {
		writeBadRequest(w)

		return false
	}

	if longer(req.Reason, maxReasonRunes) {
		writeError(w, http.StatusBadRequest, "bad_reason", "Причина длиннее 64 символов")

		return false
	}

	return true
}

func (s *Server) handlePostCityEdit(w http.ResponseWriter, r *http.Request) {
	var req cityEditRequest

	if !decodeJSON(w, r, &req) || !checkCityEdit(w, req) {
		return
	}

	ctx := r.Context()
	uid := userFrom(r).ID

	n, err := s.d.Store.CountNewCityEdits(ctx, uid)
	if err != nil {
		writeInternal(w, err)

		return
	}

	if n >= maxNewCityEdits {
		writeError(w, http.StatusTooManyRequests, "too_many_requests", "Слишком много необработанных заявок")

		return
	}

	e := store.CityEdit{
		OldCountry: req.OldCountryCode, NewCountry: req.NewCountryCode,
		OldName: req.OldName, NewName: req.NewName, Reason: req.Reason, Status: "NEW",
	}

	id, err := s.d.Store.AddCityEdit(ctx, uid, e)
	if err != nil {
		writeInternal(w, err)

		return
	}

	e.ID = id
	writeJSON(w, http.StatusCreated, toCityEditJSON(e))
}

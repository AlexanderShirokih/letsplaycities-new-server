// Package api implements the REST API v1 and static pages.
package api

import (
	"context"
	"embed"
	"encoding/json"
	"errors"
	"log"
	"net"
	"net/http"
	"strconv"
	"strings"
	"time"

	"lps/internal/auth"
	"lps/internal/dict"
	"lps/internal/store"
)

//go:embed static/*
var staticFS embed.FS

// Deps are the server's collaborators. Dict and DictPath are used by the dictionary endpoints.
type Deps struct {
	Store         *store.Store
	Verifiers     map[string]auth.Verifier
	Pepper        []byte
	NativeLimiter *auth.Limiter
	Dict          func() *dict.Dictionary
	DictPath      func() string
	Now           func() time.Time
}

type Server struct {
	d   Deps
	mux *http.ServeMux

	dicts dictCache
}

// NewServer panics if d.NativeLimiter is nil: the auth/native rate limit is mandatory.
func NewServer(d Deps) *Server {
	if d.NativeLimiter == nil {
		panic("api: Deps.NativeLimiter is required")
	}

	if d.Now == nil {
		d.Now = time.Now
	}

	s := &Server{d: d, mux: http.NewServeMux()}
	s.routes()

	return s
}

// Handler serves every /v1 route and the static pages.
func (s *Server) Handler() http.Handler { return s.mux }

func (s *Server) routes() {
	m := s.mux
	m.HandleFunc("POST /v1/auth/native", s.handleAuthNative)
	m.HandleFunc("POST /v1/auth/social", s.handleAuthSocial)
	m.HandleFunc("POST /v1/auth/logout", s.requireUser(s.handleLogout))

	m.HandleFunc("GET /v1/me", s.requireUser(s.handleGetMe))
	m.HandleFunc("PATCH /v1/me", s.requireUser(s.handlePatchMe))
	m.HandleFunc("PUT /v1/me/avatar", s.requireUser(s.handlePutAvatar))
	m.HandleFunc("DELETE /v1/me/avatar", s.requireUser(s.handleDeleteAvatar))
	m.HandleFunc("GET /v1/users/{id}", s.requireUser(s.handleGetUser))
	m.HandleFunc("GET /v1/avatars/{id}", s.handleGetAvatar)

	m.HandleFunc("GET /v1/friends", s.requireUser(s.handleListFriends))
	m.HandleFunc("PUT /v1/friends/{id}", s.requireUser(s.handlePutFriend))
	m.HandleFunc("DELETE /v1/friends/{id}", s.requireUser(s.handleDeleteFriend))
	m.HandleFunc("GET /v1/blocks", s.requireUser(s.handleListBlocks))
	m.HandleFunc("PUT /v1/blocks/{id}", s.requireUser(s.handlePutBlock))
	m.HandleFunc("DELETE /v1/blocks/{id}", s.requireUser(s.handleDeleteBlock))
	m.HandleFunc("GET /v1/history", s.requireUser(s.handleHistory))
	m.HandleFunc("GET /v1/city-edits", s.requireUser(s.handleListCityEdits))
	m.HandleFunc("POST /v1/city-edits", s.requireUser(s.handlePostCityEdit))
	m.HandleFunc("GET /v1/dictionary", s.handleDictionary)
	m.HandleFunc("GET /v1/dictionary/{file}", s.handleDictionaryFile)

	m.HandleFunc("GET /privacy-mobile.html", staticPage("static/privacy-mobile.html", "text/html; charset=utf-8"))
	m.HandleFunc("GET /legal-mobile.txt", staticPage("static/legal-mobile.txt", "text/plain; charset=utf-8"))

	m.HandleFunc("/v1/", func(w http.ResponseWriter, r *http.Request) {
		writeNotFound(w, "Не найдено")
	})
}

func staticPage(name, ctype string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		data, err := staticFS.ReadFile(name)
		if err != nil {
			writeNotFound(w, "Не найдено")

			return
		}

		w.Header().Set("Content-Type", ctype)
		_, _ = w.Write(data)
	}
}

// ---- responses ----

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, code, message string) {
	writeJSON(w, status, map[string]any{"error": map[string]string{"code": code, "message": message}})
}

func writeBadRequest(w http.ResponseWriter) {
	writeError(w, http.StatusBadRequest, "bad_request", "Некорректный запрос")
}

func writeNotFound(w http.ResponseWriter, message string) {
	writeError(w, http.StatusNotFound, "not_found", message)
}

const userNotFound = "Пользователь не найден"

// pathID parses the {id} path value. On failure it has already written
// 404 with the given message.
func pathID(w http.ResponseWriter, r *http.Request, notFound string) (int64, bool) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil {
		writeNotFound(w, notFound)

		return 0, false
	}

	return id, true
}

// writeDone answers 204, or 500 when err is set.
func writeDone(w http.ResponseWriter, err error) {
	if err != nil {
		writeInternal(w, err)

		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func writeInternal(w http.ResponseWriter, err error) {
	log.Printf("api: internal error: %v", err)
	writeError(w, http.StatusInternalServerError, "internal", "Внутренняя ошибка сервера")
}

const maxJSONBody = 64 << 10

// bodyReadTimeout bounds reading a request body, so that a client cannot
// hold a connection by trickling it. Tests shorten it.
var bodyReadTimeout = 15 * time.Second

// limitBodyTime sets the read deadline for the request body. The error is
// ignored: writers without deadline support (recorders) read without one.
func limitBodyTime(w http.ResponseWriter) {
	_ = http.NewResponseController(w).SetReadDeadline(time.Now().Add(bodyReadTimeout))
}

// decodeJSON reads a JSON body (at most 64 KB, within bodyReadTimeout) into
// v; on failure it answers 400 bad_request.
func decodeJSON(w http.ResponseWriter, r *http.Request, v any) bool {
	limitBodyTime(w)

	r.Body = http.MaxBytesReader(w, r.Body, maxJSONBody)
	if err := json.NewDecoder(r.Body).Decode(v); err != nil {
		writeBadRequest(w)

		return false
	}

	return true
}

// ---- authentication ----

var (
	ErrUnauthorized = errors.New("unauthorized")
	ErrBanned       = errors.New("banned")
)

func bearerToken(r *http.Request) string {
	h := r.Header.Get("Authorization")
	if len(h) > 7 && strings.EqualFold(h[:7], "Bearer ") {
		return strings.TrimSpace(h[7:])
	}

	return ""
}

// Authenticate resolves the Bearer token of r to a user. It returns
// ErrUnauthorized or ErrBanned (map them with WriteAuthError); any other error is internal.
func (s *Server) Authenticate(r *http.Request) (store.User, error) {
	tok := bearerToken(r)
	if tok == "" {
		return store.User{}, ErrUnauthorized
	}

	u, err := s.d.Store.UserBySession(r.Context(), auth.HashToken(tok))
	if errors.Is(err, store.ErrNotFound) {
		return store.User{}, ErrUnauthorized
	}

	if err != nil {
		return store.User{}, err
	}

	if u.Role == store.RoleBanned {
		return store.User{}, ErrBanned
	}

	return u, nil
}

// WriteAuthError writes the REST error response for an error returned by Authenticate.
func WriteAuthError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, ErrUnauthorized):
		writeError(w, http.StatusUnauthorized, "unauthorized", "Требуется вход")
	case errors.Is(err, ErrBanned):
		writeError(w, http.StatusForbidden, "banned", "Аккаунт заблокирован")
	default:
		writeInternal(w, err)
	}
}

type ctxKey struct{}

func userFrom(r *http.Request) store.User {
	u, _ := r.Context().Value(ctxKey{}).(store.User)

	return u
}

func (s *Server) requireUser(h http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		u, err := s.Authenticate(r)
		if err != nil {
			WriteAuthError(w, err)

			return
		}

		now := s.d.Now()
		if err := s.d.Store.TouchLastSeen(r.Context(), u.ID, now); err != nil {
			writeInternal(w, err)

			return
		}

		u.LastSeenAt = now
		h(w, r.WithContext(context.WithValue(r.Context(), ctxKey{}, u)))
	}
}

func clientIP(r *http.Request) string {
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}

	return host
}

// ---- user JSON ----

type userJSON struct {
	ID         int64   `json:"id"`
	Name       string  `json:"name"`
	AvatarHash *string `json:"avatarHash"`
	Provider   string  `json:"provider"`
	LastSeenAt int64   `json:"lastSeenAt"`
}

func toUserJSON(u store.User) userJSON {
	j := userJSON{ID: u.ID, Name: u.Name, Provider: u.Provider, LastSeenAt: u.LastSeenAt.UnixMilli()}
	if u.AvatarHash != "" {
		h := u.AvatarHash
		j.AvatarHash = &h
	}

	return j
}

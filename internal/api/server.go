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
	"strings"
	"time"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/auth"
	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/dict"
	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
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

	m.HandleFunc("GET /privacy-mobile.html", staticPage("static/privacy-mobile.html", "text/html; charset=utf-8"))
	m.HandleFunc("GET /legal-mobile.txt", staticPage("static/legal-mobile.txt", "text/plain; charset=utf-8"))

	m.HandleFunc("/v1/", func(w http.ResponseWriter, r *http.Request) {
		writeError(w, http.StatusNotFound, "not_found", "Не найдено")
	})
}

func staticPage(name, ctype string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		data, err := staticFS.ReadFile(name)
		if err != nil {
			writeError(w, http.StatusNotFound, "not_found", "Не найдено")

			return
		}

		w.Header().Set("Content-Type", ctype)
		w.Write(data)
	}
}

// ---- responses ----

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, code, message string) {
	writeJSON(w, status, map[string]any{"error": map[string]string{"code": code, "message": message}})
}

func writeInternal(w http.ResponseWriter, err error) {
	log.Printf("api: internal error: %v", err)
	writeError(w, http.StatusInternalServerError, "internal", "Внутренняя ошибка сервера")
}

const maxJSONBody = 64 << 10

// decodeJSON reads a JSON body (at most 64 KB) into v; on failure it answers 400 bad_request.
func decodeJSON(w http.ResponseWriter, r *http.Request, v any) bool {
	r.Body = http.MaxBytesReader(w, r.Body, maxJSONBody)
	if err := json.NewDecoder(r.Body).Decode(v); err != nil {
		writeError(w, http.StatusBadRequest, "bad_request", "Некорректный запрос")

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

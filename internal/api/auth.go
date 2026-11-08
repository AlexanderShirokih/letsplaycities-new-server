package api

import (
	"context"
	"errors"
	"log"
	"net/http"
	"strings"
	"unicode/utf8"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/auth"
	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

type authResponse struct {
	Token string   `json:"token"`
	User  userJSON `json:"user"`
}

// checkName trims name and validates it; on failure it writes the error and returns ok=false.
func (s *Server) checkName(ctx context.Context, w http.ResponseWriter, name string) (string, bool) {
	name = strings.TrimSpace(name)
	if n := utf8.RuneCountInString(name); n < 2 || n > 64 {
		writeError(w, http.StatusBadRequest, "bad_name", "Имя должно содержать от 2 до 64 символов")

		return "", false
	}

	forbidden, err := s.d.Store.IsForbiddenName(ctx, name)
	if err != nil {
		writeInternal(w, err)

		return "", false
	}

	if forbidden {
		writeError(w, http.StatusBadRequest, "forbidden_name", "Это имя использовать нельзя")

		return "", false
	}

	return name, true
}

// login finds or creates the account for (provider, uid) and answers {token, user}.
// name is used only when the account is created.
func (s *Server) login(w http.ResponseWriter, r *http.Request, provider, uid, name string) {
	ctx := r.Context()

	u, err := s.d.Store.UserByIdentity(ctx, provider, uid)
	if errors.Is(err, store.ErrNotFound) {
		var ok bool

		if name, ok = s.checkName(ctx, w, name); !ok {
			return
		}

		u, err = s.d.Store.CreateUser(ctx, provider, uid, name, s.d.Now())
		if err != nil {
			// A concurrent login may have created the same identity.
			if again, err2 := s.d.Store.UserByIdentity(ctx, provider, uid); err2 == nil {
				u, err = again, nil
			}
		}
	}

	if err != nil {
		writeInternal(w, err)

		return
	}

	if u.Role == store.RoleBanned {
		WriteAuthError(w, ErrBanned)

		return
	}

	token, hash := auth.NewToken()
	if err := s.d.Store.CreateSession(ctx, hash, u.ID, s.d.Now()); err != nil {
		writeInternal(w, err)

		return
	}

	writeJSON(w, http.StatusOK, authResponse{Token: token, User: toUserJSON(u)})
}

func (s *Server) handleAuthNative(w http.ResponseWriter, r *http.Request) {
	if !s.d.NativeLimiter.Allow(clientIP(r)) {
		writeError(w, http.StatusTooManyRequests, "too_many_requests", "Слишком много запросов, попробуйте позже")

		return
	}

	var req struct{ DeviceID, Name string }

	if !decodeJSON(w, r, &req) {
		return
	}

	if req.DeviceID == "" {
		writeBadRequest(w)

		return
	}

	s.login(w, r, "native", auth.NativeUID(s.d.Pepper, req.DeviceID), req.Name)
}

func (s *Server) handleAuthSocial(w http.ResponseWriter, r *http.Request) {
	var req struct{ Provider, Token, Name string }
	if !decodeJSON(w, r, &req) {
		return
	}

	if req.Provider == "" || req.Token == "" {
		writeBadRequest(w)

		return
	}

	v, ok := s.d.Verifiers[req.Provider]
	if !ok {
		writeError(w, http.StatusBadRequest, "provider_disabled", "Вход через эту соцсеть недоступен")

		return
	}

	uid, err := v.Verify(r.Context(), req.Token)
	if errors.Is(err, auth.ErrInvalidToken) {
		writeError(w, http.StatusUnauthorized, "invalid_provider_token", "Не удалось подтвердить вход через соцсеть")

		return
	}

	if err != nil {
		log.Printf("api: %s verify: %v", req.Provider, err)

		writeError(w, http.StatusBadGateway, "provider_unavailable", "Соцсеть временно недоступна")

		return
	}

	s.login(w, r, req.Provider, uid, req.Name)
}

func (s *Server) handleLogout(w http.ResponseWriter, r *http.Request) {
	if err := s.d.Store.DeleteSession(r.Context(), auth.HashToken(bearerToken(r))); err != nil {
		writeInternal(w, err)

		return
	}

	w.WriteHeader(http.StatusNoContent)
}

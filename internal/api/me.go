package api

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"image"
	_ "image/jpeg"
	_ "image/png"
	"io"
	"mime"
	"net/http"
	"strconv"
	"strings"

	"lps/internal/store"
)

const (
	maxAvatar     = 256 << 10
	maxAvatarSide = 512
)

func (s *Server) handleGetMe(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, toUserJSON(userFrom(r)))
}

func (s *Server) handlePatchMe(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Name     *string `json:"name"`
		FCMToken *string `json:"fcmToken"`
	}

	if !decodeJSON(w, r, &req) {
		return
	}

	ctx := r.Context()
	u := userFrom(r)

	if req.Name != nil {
		name, ok := s.checkName(ctx, w, *req.Name)
		if !ok {
			return
		}

		if err := s.d.Store.UpdateName(ctx, u.ID, name); err != nil {
			writeInternal(w, err)

			return
		}

		u.Name = name
	}

	if req.FCMToken != nil {
		if err := s.d.Store.UpdateFCMToken(ctx, u.ID, *req.FCMToken); err != nil {
			writeInternal(w, err)

			return
		}

		u.FCMToken = *req.FCMToken
	}

	writeJSON(w, http.StatusOK, toUserJSON(u))
}

// validAvatar reports whether data is a PNG or JPEG of type mt with sides of at most maxAvatarSide.
func validAvatar(data []byte, mt string) bool {
	cfg, format, err := image.DecodeConfig(bytes.NewReader(data))
	if err != nil || "image/"+format != mt {
		return false
	}

	return cfg.Width <= maxAvatarSide && cfg.Height <= maxAvatarSide
}

func (s *Server) handlePutAvatar(w http.ResponseWriter, r *http.Request) {
	data, err := io.ReadAll(http.MaxBytesReader(w, r.Body, maxAvatar))
	if err != nil {
		var tooBig *http.MaxBytesError

		if errors.As(err, &tooBig) {
			writeError(w, http.StatusRequestEntityTooLarge, "too_large", "Файл больше 256 КБ")

			return
		}

		writeBadRequest(w)

		return
	}

	mt, _, _ := mime.ParseMediaType(r.Header.Get("Content-Type"))

	if !validAvatar(data, mt) {
		writeError(w, http.StatusBadRequest, "bad_image", "Нужна картинка PNG или JPEG не больше 512×512")

		return
	}

	sum := sha256.Sum256(data)
	hash := hex.EncodeToString(sum[:])[:16]

	u := userFrom(r)
	if err := s.d.Store.SetAvatar(r.Context(), u.ID, store.Avatar{MIME: mt, Data: data}, hash); err != nil {
		writeInternal(w, err)

		return
	}

	u.AvatarHash = hash
	writeJSON(w, http.StatusOK, toUserJSON(u))
}

func (s *Server) handleDeleteAvatar(w http.ResponseWriter, r *http.Request) {
	if err := s.d.Store.DeleteAvatar(r.Context(), userFrom(r).ID); err != nil {
		writeInternal(w, err)

		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (s *Server) handleGetAvatar(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r, "Аватарка не найдена")
	if !ok {
		return
	}

	av, hash, err := s.d.Store.Avatar(r.Context(), id)
	if errors.Is(err, store.ErrNotFound) {
		writeNotFound(w, "Аватарка не найдена")

		return
	}

	if err != nil {
		writeInternal(w, err)

		return
	}

	etag := `"` + hash + `"`
	h := w.Header()
	h.Set("ETag", etag)

	if r.URL.Query().Get("v") == hash {
		h.Set("Cache-Control", "public, max-age=31536000, immutable")
	} else {
		h.Set("Cache-Control", "no-cache")
	}

	for _, c := range strings.Split(r.Header.Get("If-None-Match"), ",") {
		c = strings.TrimPrefix(strings.TrimSpace(c), "W/")
		if c == etag || c == "*" {
			w.WriteHeader(http.StatusNotModified)

			return
		}
	}

	h.Set("Content-Type", av.MIME)
	h.Set("Content-Length", strconv.Itoa(len(av.Data)))
	_, _ = w.Write(av.Data)
}

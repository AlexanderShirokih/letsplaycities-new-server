package api

import (
	"crypto/sha256"
	"encoding/hex"
	"io"
	"net/http"
	"os"
	"strconv"
	"strings"
	"sync"
)

type dictMeta struct {
	Size   int64
	SHA256 string
}

// dictCache holds the size and checksum of dictionary files, computed once per path.
type dictCache struct {
	mu sync.Mutex
	m  map[string]dictMeta
}

func (c *dictCache) meta(path string) (dictMeta, error) {
	c.mu.Lock()
	defer c.mu.Unlock()

	if m, ok := c.m[path]; ok {
		return m, nil
	}

	f, err := os.Open(path)
	if err != nil {
		return dictMeta{}, err
	}
	defer func() { _ = f.Close() }()

	h := sha256.New()

	n, err := io.Copy(h, f)
	if err != nil {
		return dictMeta{}, err
	}

	m := dictMeta{Size: n, SHA256: hex.EncodeToString(h.Sum(nil))}
	if c.m == nil {
		c.m = map[string]dictMeta{}
	}

	c.m[path] = m

	return m, nil
}

func (s *Server) handleDictionary(w http.ResponseWriter, _ *http.Request) {
	if s.d.Dict == nil || s.d.DictPath == nil {
		writeError(w, http.StatusNotFound, "not_found", "Словарь не загружен")

		return
	}

	m, err := s.dicts.meta(s.d.DictPath())
	if err != nil {
		writeInternal(w, err)

		return
	}

	writeJSON(w, http.StatusOK, map[string]any{"version": s.d.Dict().Version, "size": m.Size, "sha256": m.SHA256})
}

func (s *Server) handleDictionaryFile(w http.ResponseWriter, r *http.Request) {
	name, ok := strings.CutSuffix(r.PathValue("file"), ".db2")
	v, err := strconv.Atoi(name)

	if !ok || err != nil || s.d.Dict == nil || s.d.DictPath == nil || v != s.d.Dict().Version {
		writeError(w, http.StatusNotFound, "not_found", "Версия словаря не найдена")

		return
	}

	f, err := os.Open(s.d.DictPath())
	if err != nil {
		writeInternal(w, err)

		return
	}
	defer func() { _ = f.Close() }()

	st, err := f.Stat()
	if err != nil {
		writeInternal(w, err)

		return
	}

	w.Header().Set("Content-Type", "application/octet-stream")
	w.Header().Set("Cache-Control", "public, max-age=31536000, immutable")
	http.ServeContent(w, r, "", st.ModTime(), f)
}

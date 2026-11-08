package api

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"os"
	"testing"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/dict"
)

const testDictPath = "../../testdata/dict/data-80.db2"

func TestDictionaryEndpoints(t *testing.T) {
	raw, err := os.ReadFile(testDictPath)
	if err != nil {
		t.Fatal(err)
	}

	d, err := dict.Parse(bytes.NewReader(raw))
	if err != nil {
		t.Fatal(err)
	}

	e := newTestServerWith(t, 1000, func(deps *Deps) {
		deps.Dict = func() *dict.Dictionary { return d }
		deps.DictPath = func() string { return testDictPath }
	})

	resp, data := e.do("GET", "/v1/dictionary", "", nil)
	if resp.StatusCode != 200 {
		t.Fatalf("meta: %d %s", resp.StatusCode, data)
	}

	var meta struct {
		Version int    `json:"version"`
		Size    int64  `json:"size"`
		SHA256  string `json:"sha256"`
	}

	if err := json.Unmarshal(data, &meta); err != nil {
		t.Fatal(err)
	}

	sum := sha256.Sum256(raw)
	if meta.Version != 80 || meta.Size != int64(len(raw)) || meta.SHA256 != hex.EncodeToString(sum[:]) {
		t.Fatalf("meta: %+v", meta)
	}

	resp, data = e.do("GET", "/v1/dictionary/80.db2", "", nil)
	if resp.StatusCode != 200 || !bytes.Equal(data, raw) {
		t.Fatalf("file: %d, %d bytes", resp.StatusCode, len(data))
	}

	if cc := resp.Header.Get("Cache-Control"); cc != "public, max-age=31536000, immutable" {
		t.Fatalf("Cache-Control %q", cc)
	}

	for _, p := range []string{"/v1/dictionary/79.db2", "/v1/dictionary/80", "/v1/dictionary/x.db2"} {
		resp, data = e.do("GET", p, "", nil)
		e.wantError(resp, data, 404, "not_found")
	}
}

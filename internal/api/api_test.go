package api

import (
	"bytes"
	"context"
	"database/sql"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"lps/internal/auth"
	"lps/internal/store"
)

// fakeVerifier accepts tokens of the form "good-<uid>".
type fakeVerifier struct{}

func (fakeVerifier) Verify(_ context.Context, token string) (string, error) {
	if uid, ok := strings.CutPrefix(token, "good-"); ok {
		return uid, nil
	}

	return "", auth.ErrInvalidToken
}

type testEnv struct {
	t      *testing.T
	ts     *httptest.Server
	store  *store.Store
	dbPath string
}

func newTestServer(t *testing.T) *testEnv { return newTestServerLimit(t, 1000) }

func newTestServerLimit(t *testing.T, limit int) *testEnv {
	t.Helper()

	return newTestServerWith(t, limit, nil)
}

// newTestServerWith builds a server; tweak, when set, adjusts the dependencies before use.
func newTestServerWith(t *testing.T, limit int, tweak func(*Deps)) *testEnv {
	t.Helper()
	dbPath := filepath.Join(t.TempDir(), "t.db")

	st, err := store.Open(dbPath)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() { _ = st.Close() })

	deps := Deps{
		Store:         st,
		Verifiers:     map[string]auth.Verifier{"google": fakeVerifier{}},
		Pepper:        []byte("pepper"),
		NativeLimiter: auth.NewLimiter(limit, time.Minute, time.Now),
		Now:           time.Now,
	}

	if tweak != nil {
		tweak(&deps)
	}

	srv := NewServer(deps)

	ts := httptest.NewServer(srv.Handler())
	t.Cleanup(ts.Close)

	return &testEnv{t: t, ts: ts, store: st, dbPath: dbPath}
}

// sql runs a statement through a second connection (the store exposes no admin writes).
func (e *testEnv) sql(q string, args ...any) {
	e.t.Helper()

	db, err := sql.Open("sqlite", "file:"+e.dbPath+"?_pragma=busy_timeout(5000)")
	if err != nil {
		e.t.Fatal(err)
	}

	defer func() { _ = db.Close() }()

	if _, err := db.Exec(q, args...); err != nil {
		e.t.Fatal(err)
	}
}

// do sends a request; body may be nil, []byte (raw) or any JSON value.
func (e *testEnv) do(method, path, token string, body any, hdr ...string) (*http.Response, []byte) {
	e.t.Helper()

	var rd io.Reader

	ct := ""

	switch b := body.(type) {
	case nil:
	case []byte:
		rd = bytes.NewReader(b)
	default:
		raw, err := json.Marshal(b)
		if err != nil {
			e.t.Fatal(err)
		}

		rd = bytes.NewReader(raw)
		ct = "application/json"
	}

	req, err := http.NewRequest(method, e.ts.URL+path, rd)
	if err != nil {
		e.t.Fatal(err)
	}

	if ct != "" {
		req.Header.Set("Content-Type", ct)
	}

	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}

	for i := 0; i+1 < len(hdr); i += 2 {
		req.Header.Set(hdr[i], hdr[i+1])
	}

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		e.t.Fatal(err)
	}

	defer func() { _ = resp.Body.Close() }()

	data, _ := io.ReadAll(resp.Body)

	return resp, data
}

type testUser struct {
	ID         int64   `json:"id"`
	Name       string  `json:"name"`
	AvatarHash *string `json:"avatarHash"`
	Provider   string  `json:"provider"`
	LastSeenAt int64   `json:"lastSeenAt"`
}

type authResp struct {
	Token string   `json:"token"`
	User  testUser `json:"user"`
}

func (e *testEnv) wantError(resp *http.Response, data []byte, status int, code string) {
	e.t.Helper()

	if resp.StatusCode != status {
		e.t.Fatalf("status %d, want %d: %s", resp.StatusCode, status, data)
	}

	var er struct {
		Error struct{ Code, Message string } `json:"error"`
	}

	if err := json.Unmarshal(data, &er); err != nil || er.Error.Code != code || er.Error.Message == "" {
		e.t.Fatalf("error body %s, want code %q with message", data, code)
	}
}

func (e *testEnv) native(device, name string) authResp {
	e.t.Helper()

	resp, data := e.do("POST", "/v1/auth/native", "", map[string]string{"deviceId": device, "name": name})
	if resp.StatusCode != 200 {
		e.t.Fatalf("native: %d %s", resp.StatusCode, data)
	}

	var a authResp
	if err := json.Unmarshal(data, &a); err != nil {
		e.t.Fatal(err)
	}

	return a
}

func (e *testEnv) me(token string) testUser {
	e.t.Helper()

	resp, data := e.do("GET", "/v1/me", token, nil)
	if resp.StatusCode != 200 {
		e.t.Fatalf("me: %d %s", resp.StatusCode, data)
	}

	var u testUser
	if err := json.Unmarshal(data, &u); err != nil {
		e.t.Fatal(err)
	}

	return u
}

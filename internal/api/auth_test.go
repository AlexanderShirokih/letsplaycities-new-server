package api

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/auth"
)

func TestNativeCreatesThenFinds(t *testing.T) {
	e := newTestServer(t)
	a := e.native("dev1", "Вася")
	if a.Token == "" || a.User.Name != "Вася" || a.User.Provider != "native" || a.User.AvatarHash != nil || a.User.LastSeenAt == 0 {
		t.Fatalf("bad response: %+v", a)
	}

	b := e.native("dev1", "Петя")
	if b.User.ID != a.User.ID || b.User.Name != "Вася" {
		t.Fatalf("second login: %+v", b)
	}
}

func TestNativeBadName(t *testing.T) {
	e := newTestServer(t)
	e.sql(`INSERT INTO forbidden_logins(value) VALUES('admin')`)
	resp, data := e.do("POST", "/v1/auth/native", "", map[string]string{"deviceId": "d", "name": "a"})
	e.wantError(resp, data, 400, "bad_name")
	resp, data = e.do("POST", "/v1/auth/native", "", map[string]string{"deviceId": "d", "name": "Super Admin"})
	e.wantError(resp, data, 400, "forbidden_name")
}

func TestBadRequest(t *testing.T) {
	e := newTestServer(t)
	resp, data := e.do("POST", "/v1/auth/native", "", []byte("{not json"))
	e.wantError(resp, data, 400, "bad_request")
	resp, data = e.do("POST", "/v1/auth/native", "", map[string]string{"name": "Вася"})
	e.wantError(resp, data, 400, "bad_request")
}

func TestNativeRateLimit(t *testing.T) {
	e := newTestServerLimit(t, 10)
	for i := 0; i < 10; i++ {
		e.native("dev", "Вася")
	}

	resp, data := e.do("POST", "/v1/auth/native", "", map[string]string{"deviceId": "dev", "name": "Вася"})
	e.wantError(resp, data, 429, "too_many_requests")
}

func TestSocialDisabledProvider(t *testing.T) {
	e := newTestServer(t)
	resp, data := e.do("POST", "/v1/auth/social", "", map[string]string{"provider": "vk", "token": "good-1", "name": "Вася"})
	e.wantError(resp, data, 400, "provider_disabled")
}

func TestSocialInvalidToken(t *testing.T) {
	e := newTestServer(t)
	resp, data := e.do("POST", "/v1/auth/social", "", map[string]string{"provider": "google", "token": "bad", "name": "Вася"})
	e.wantError(resp, data, 401, "invalid_provider_token")
}

func TestSocialLogin(t *testing.T) {
	e := newTestServer(t)
	body := map[string]string{"provider": "google", "token": "good-42", "name": "Вася"}
	resp, data := e.do("POST", "/v1/auth/social", "", body)
	if resp.StatusCode != 200 {
		t.Fatalf("%d %s", resp.StatusCode, data)
	}

	var a, b authResp
	json.Unmarshal(data, &a)
	if a.User.Provider != "google" || a.Token == "" {
		t.Fatalf("%+v", a)
	}

	body["name"] = "Другой"
	_, data = e.do("POST", "/v1/auth/social", "", body)
	json.Unmarshal(data, &b)
	if b.User.ID != a.User.ID || b.User.Name != "Вася" {
		t.Fatalf("%+v", b)
	}
}

func TestBannedUser(t *testing.T) {
	e := newTestServer(t)
	a := e.native("dev1", "Вася")
	e.sql(`UPDATE users SET role = 0 WHERE id = ?`, a.User.ID)
	resp, data := e.do("POST", "/v1/auth/native", "", map[string]string{"deviceId": "dev1", "name": "Вася"})
	e.wantError(resp, data, 403, "banned")
	resp, data = e.do("GET", "/v1/me", a.Token, nil)
	e.wantError(resp, data, 403, "banned")
}

func TestUnauthorized(t *testing.T) {
	e := newTestServer(t)
	resp, data := e.do("GET", "/v1/me", "", nil)
	e.wantError(resp, data, 401, "unauthorized")
	resp, data = e.do("GET", "/v1/me", "garbage", nil)
	e.wantError(resp, data, 401, "unauthorized")
}

func TestLogout(t *testing.T) {
	e := newTestServer(t)
	a := e.native("dev1", "Вася")
	resp, _ := e.do("POST", "/v1/auth/logout", a.Token, nil)
	if resp.StatusCode != 204 {
		t.Fatalf("logout status %d", resp.StatusCode)
	}

	resp, data := e.do("GET", "/v1/me", a.Token, nil)
	e.wantError(resp, data, 401, "unauthorized")
}

func TestNewServerRequiresLimiter(t *testing.T) {
	defer func() {
		if recover() == nil {
			t.Fatal("expected panic for nil NativeLimiter")
		}
	}()

	NewServer(Deps{})
}

type failingVerifier struct{}

func (failingVerifier) Verify(context.Context, string) (string, error) {
	return "", errors.New("dial tcp secret-host: refused")
}

func TestSocialProviderUnavailableIsLogged(t *testing.T) {
	e := newTestServer(t)
	srv := NewServer(Deps{
		Store:         e.store,
		Verifiers:     map[string]auth.Verifier{"vk": failingVerifier{}},
		Pepper:        []byte("p"),
		NativeLimiter: auth.NewLimiter(10, time.Minute, time.Now),
	})
	ts := httptest.NewServer(srv.Handler())
	defer ts.Close()

	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer log.SetOutput(os.Stderr)

	resp, err := http.Post(ts.URL+"/v1/auth/social", "application/json",
		strings.NewReader(`{"provider":"vk","token":"x","name":"Вася"}`))
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)

	e.wantError(resp, body, 502, "provider_unavailable")
	if strings.Contains(string(body), "secret-host") {
		t.Fatalf("provider error leaked to client: %s", body)
	}
	if !strings.Contains(buf.String(), "vk verify") || !strings.Contains(buf.String(), "secret-host") {
		t.Fatalf("error not logged: %q", buf.String())
	}
}

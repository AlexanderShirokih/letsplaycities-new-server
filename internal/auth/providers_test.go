package auth

import (
	"context"
	"crypto/md5"
	"crypto/rand"
	"crypto/rsa"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"math/big"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync/atomic"
	"testing"
	"time"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/config"
	"github.com/golang-jwt/jwt/v5"
)

func jwk(kid string, k *rsa.PublicKey) map[string]string {
	return map[string]string{
		"kty": "RSA", "alg": "RS256", "use": "sig", "kid": kid,
		"n": base64.RawURLEncoding.EncodeToString(k.N.Bytes()),
		"e": base64.RawURLEncoding.EncodeToString(big.NewInt(int64(k.E)).Bytes()),
	}
}

func signGoogle(t *testing.T, key *rsa.PrivateKey, kid, iss, aud string, exp time.Time) string {
	t.Helper()

	tok := jwt.NewWithClaims(jwt.SigningMethodRS256, jwt.MapClaims{
		"iss": iss, "aud": aud, "sub": "g-123", "exp": exp.Unix(),
	})
	tok.Header["kid"] = kid

	s, err := tok.SignedString(key)
	if err != nil {
		t.Fatal(err)
	}

	return s
}

func TestGoogleVerify(t *testing.T) {
	key, _ := rsa.GenerateKey(rand.Reader, 2048)
	other, _ := rsa.GenerateKey(rand.Reader, 2048)

	var hits atomic.Int32

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		hits.Add(1)
		w.Header().Set("Cache-Control", "public, max-age=3600")
		writeJSON(w, map[string]any{"keys": []any{jwk("k1", &key.PublicKey)}})
	}))
	defer srv.Close()

	g := &Google{JWKSURL: srv.URL, ClientIDs: []string{"cid"}, HTTP: srv.Client()}
	ctx := context.Background()
	future := time.Now().Add(time.Hour)

	uid, err := g.Verify(ctx, signGoogle(t, key, "k1", "https://accounts.google.com", "cid", future))
	if err != nil || uid != "g-123" {
		t.Fatalf("valid: %q %v", uid, err)
	}

	if uid, err = g.Verify(ctx, signGoogle(t, key, "k1", "accounts.google.com", "cid", future)); err != nil || uid != "g-123" {
		t.Fatalf("bare iss: %q %v", uid, err)
	}

	if hits.Load() != 1 {
		t.Fatalf("jwks fetched %d times, want cached 1", hits.Load())
	}

	bad := map[string]string{
		"aud":     signGoogle(t, key, "k1", "https://accounts.google.com", "evil", future),
		"expired": signGoogle(t, key, "k1", "https://accounts.google.com", "cid", time.Now().Add(-time.Hour)),
		"sig":     signGoogle(t, other, "k1", "https://accounts.google.com", "cid", future),
		"iss":     signGoogle(t, key, "k1", "https://evil.example", "cid", future),
		"garbage": "not.a.jwt",
	}
	for name, tok := range bad {
		if _, err := g.Verify(ctx, tok); !errors.Is(err, ErrInvalidToken) {
			t.Errorf("%s: err = %v, want ErrInvalidToken", name, err)
		}
	}
}

func TestGoogleRefetchOnUnknownKid(t *testing.T) {
	k1, _ := rsa.GenerateKey(rand.Reader, 2048)
	k2, _ := rsa.GenerateKey(rand.Reader, 2048)

	var hits atomic.Int32

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		n := hits.Add(1)

		w.Header().Set("Cache-Control", "max-age=3600")

		keys := []any{jwk("k1", &k1.PublicKey)}
		if n > 1 {
			keys = append(keys, jwk("k2", &k2.PublicKey))
		}

		writeJSON(w, map[string]any{"keys": keys})
	}))
	defer srv.Close()

	g := &Google{JWKSURL: srv.URL, ClientIDs: []string{"cid"}, HTTP: srv.Client()}
	ctx := context.Background()

	future := time.Now().Add(time.Hour)
	if _, err := g.Verify(ctx, signGoogle(t, k1, "k1", "accounts.google.com", "cid", future)); err != nil {
		t.Fatal(err)
	}

	if _, err := g.Verify(ctx, signGoogle(t, k2, "k2", "accounts.google.com", "cid", future)); err != nil {
		t.Fatalf("rotated key: %v", err)
	}

	if _, err := g.Verify(ctx, signGoogle(t, k2, "nope", "accounts.google.com", "cid", future)); !errors.Is(err, ErrInvalidToken) {
		t.Fatalf("unknown kid: %v", err)
	}
}

func TestGoogleJWKSServerError(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(500) }))
	defer srv.Close()

	key, _ := rsa.GenerateKey(rand.Reader, 2048)
	g := &Google{JWKSURL: srv.URL, ClientIDs: []string{"cid"}, HTTP: srv.Client()}

	_, err := g.Verify(context.Background(), signGoogle(t, key, "k1", "accounts.google.com", "cid", time.Now().Add(time.Hour)))
	if err == nil || errors.Is(err, ErrInvalidToken) {
		t.Fatalf("err = %v, want non-ErrInvalidToken", err)
	}
}

// providerCases runs the shared success / rejection / 500 checks.
func providerCases(t *testing.T, mk func(url string, hc *http.Client) Verifier, ok, reject string) {
	t.Helper()

	for _, c := range []struct {
		name    string
		status  int
		body    string
		uid     string
		invalid bool
		fail    bool
	}{
		{"ok", 200, ok, "42", false, false},
		{"reject", 200, reject, "", true, false},
		{"500", 500, "oops", "", false, true},
		{"garbage", 200, "<html>", "", false, true},
	} {
		t.Run(c.name, func(t *testing.T) {
			srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.WriteHeader(c.status)
				_, _ = w.Write([]byte(c.body))
			}))
			defer srv.Close()

			uid, err := mk(srv.URL, srv.Client()).Verify(context.Background(), "tok")

			switch {
			case c.invalid:
				if !errors.Is(err, ErrInvalidToken) {
					t.Fatalf("err = %v", err)
				}
			case c.fail:
				if err == nil || errors.Is(err, ErrInvalidToken) {
					t.Fatalf("err = %v, want non-ErrInvalidToken", err)
				}
			default:
				if err != nil || uid != c.uid {
					t.Fatalf("uid %q err %v", uid, err)
				}
			}
		})
	}
}

func TestVKVerify(t *testing.T) {
	providerCases(t, func(u string, hc *http.Client) Verifier {
		return &VK{BaseURL: u, ServiceToken: "svc", HTTP: hc}
	}, `{"response":{"success":1,"user_id":42}}`, `{"error":{"error_code":15}}`)

	var q map[string][]string

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		q = r.URL.Query()

		_, _ = w.Write([]byte(`{"response":{"success":0}}`))
	}))
	defer srv.Close()

	_, err := (&VK{BaseURL: srv.URL, ServiceToken: "svc", HTTP: srv.Client()}).Verify(context.Background(), "usertok")
	if !errors.Is(err, ErrInvalidToken) {
		t.Fatal(err)
	}

	if q["token"][0] != "usertok" || q["access_token"][0] != "svc" || q["v"][0] != "5.199" {
		t.Fatalf("query %v", q)
	}
}

func TestOKVerify(t *testing.T) {
	providerCases(t, func(u string, hc *http.Client) Verifier {
		return &OK{BaseURL: u, AppKey: "key", AppSecret: "secret", HTTP: hc}
	}, `{"uid":"42"}`, `{"error_code":100,"error_msg":"PARAM_API_KEY"}`)

	var got map[string][]string

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		got = r.URL.Query()

		_, _ = w.Write([]byte(`{"uid":"7"}`))
	}))
	defer srv.Close()

	v := &OK{BaseURL: srv.URL, AppKey: "key", AppSecret: "secret", HTTP: srv.Client()}
	if uid, err := v.Verify(context.Background(), "atok"); err != nil || uid != "7" {
		t.Fatalf("%q %v", uid, err)
	}

	md := func(s string) string { h := md5.Sum([]byte(s)); return hex.EncodeToString(h[:]) }

	want := md("application_key=keyformat=jsonmethod=users.getCurrentUser" + md("atok"+"secret"))
	if got["sig"][0] != want {
		t.Fatalf("sig %q want %q", got["sig"][0], want)
	}

	if got["access_token"][0] != "atok" || got["application_key"][0] != "key" {
		t.Fatalf("query %v", got)
	}
}

func TestFBVerify(t *testing.T) {
	providerCases(t, func(u string, hc *http.Client) Verifier {
		return &FB{BaseURL: u, AppID: "app", AppSecret: "sec", HTTP: hc}
	}, `{"data":{"is_valid":true,"app_id":"app","user_id":"42"}}`, `{"data":{"is_valid":false,"app_id":"app"}}`)

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Query().Get("access_token") != "app|sec" || r.URL.Query().Get("input_token") != "tok" {
			t.Errorf("query %v", r.URL.Query())
		}

		_, _ = w.Write([]byte(`{"data":{"is_valid":true,"app_id":"other","user_id":"1"}}`))
	}))
	defer srv.Close()

	_, err := (&FB{BaseURL: srv.URL, AppID: "app", AppSecret: "sec", HTTP: srv.Client()}).Verify(context.Background(), "tok")
	if !errors.Is(err, ErrInvalidToken) {
		t.Fatalf("foreign app: %v", err)
	}
}

func TestNewVerifiersOnlyConfigured(t *testing.T) {
	m := NewVerifiers(config.Config{VKServiceToken: "x"}, http.DefaultClient)
	if len(m) != 1 || m["vk"] == nil {
		t.Fatalf("%v", m)
	}

	all := NewVerifiers(config.Config{
		GoogleClientIDs: []string{"a"}, VKServiceToken: "x",
		OKAppKey: "k", OKAppSecret: "s", FBAppID: "i", FBAppSecret: "s",
	}, http.DefaultClient)
	if len(all) != 4 {
		t.Fatalf("%v", all)
	}

	if len(NewVerifiers(config.Config{OKAppKey: "k"}, http.DefaultClient)) != 0 {
		t.Fatal("half-configured OK enabled")
	}
}

func writeJSON(w http.ResponseWriter, v any) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(v)
}

func TestProvider4xxIsRejection(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(400)
		_, _ = w.Write([]byte(`{"error":{"message":"bad token"}}`))
	}))
	defer srv.Close()

	_, err := (&FB{BaseURL: srv.URL, AppID: "a", AppSecret: "s", HTTP: srv.Client()}).Verify(context.Background(), "t")
	if !errors.Is(err, ErrInvalidToken) {
		t.Fatalf("err = %v", err)
	}
}

func TestProviderErrorsDoNotLeakSecrets(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {}))
	url, hc := srv.URL, srv.Client()
	srv.Close() // connection refused

	cancelled, cancel := context.WithCancel(context.Background())
	cancel()

	const user, secret = "USERTOKEN123", "SECRET456"

	vs := map[string]Verifier{
		"vk": &VK{BaseURL: url, ServiceToken: secret, HTTP: hc},
		"ok": &OK{BaseURL: url, AppKey: "key", AppSecret: secret, HTTP: hc},
		"fb": &FB{BaseURL: url, AppID: "app", AppSecret: secret, HTTP: hc},
	}
	for name, v := range vs {
		for cname, ctx := range map[string]context.Context{"closed": context.Background(), "cancelled": cancelled} {
			_, err := v.Verify(ctx, user)
			if err == nil || errors.Is(err, ErrInvalidToken) {
				t.Fatalf("%s/%s: err = %v", name, cname, err)
			}

			if msg := err.Error(); strings.Contains(msg, secret) || strings.Contains(msg, user) || strings.Contains(msg, "access_token") {
				t.Errorf("%s/%s leaks: %s", name, cname, msg)
			}

			if cname == "cancelled" && !errors.Is(err, context.Canceled) {
				t.Errorf("%s: lost context.Canceled: %v", name, err)
			}
		}
	}
}

package auth

import (
	"context"
	"crypto/md5"
	"crypto/rsa"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"math/big"
	"net/http"
	"net/url"
	"slices"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/config"
	"github.com/golang-jwt/jwt/v5"
)

// ErrInvalidToken means the provider explicitly rejected the token.
var ErrInvalidToken = errors.New("invalid provider token")

// Verifier checks a provider token and returns the provider-side user id.
type Verifier interface {
	Verify(ctx context.Context, token string) (uid string, err error)
}

const (
	googleJWKSURL = "https://www.googleapis.com/oauth2/v3/certs"
	vkURL         = "https://api.vk.com/method/secure.checkToken"
	okURL         = "https://api.ok.ru/fb.do"
	fbURL         = "https://graph.facebook.com/debug_token"
)

// NewVerifiers builds verifiers for the configured providers only.
func NewVerifiers(cfg config.Config, hc *http.Client) map[string]Verifier {
	m := map[string]Verifier{}
	if len(cfg.GoogleClientIDs) > 0 {
		m["google"] = &Google{JWKSURL: googleJWKSURL, ClientIDs: cfg.GoogleClientIDs, HTTP: hc, MinRefetch: time.Minute}
	}

	if cfg.VKServiceToken != "" {
		m["vk"] = &VK{BaseURL: vkURL, ServiceToken: cfg.VKServiceToken, HTTP: hc}
	}

	if cfg.OKAppKey != "" && cfg.OKAppSecret != "" {
		m["ok"] = &OK{BaseURL: okURL, AppKey: cfg.OKAppKey, AppSecret: cfg.OKAppSecret, HTTP: hc}
	}

	if cfg.FBAppID != "" && cfg.FBAppSecret != "" {
		m["fb"] = &FB{BaseURL: fbURL, AppID: cfg.FBAppID, AppSecret: cfg.FBAppSecret, HTTP: hc}
	}

	return m
}

type statusError struct{ code int }

func (e *statusError) Error() string { return fmt.Sprintf("provider status %d", e.code) }

// rejection maps a provider 4xx answer (except 408/429) to ErrInvalidToken;
// other errors pass through unchanged.
func rejection(err error) error {
	var se *statusError
	if errors.As(err, &se) && se.code >= 400 && se.code < 500 && se.code != 408 && se.code != 429 {
		return ErrInvalidToken
	}

	return err
}

// stripURL drops the request URL (it carries tokens in its query) from a
// *url.Error, keeping the underlying cause for errors.Is.
func stripURL(err error) error {
	var ue *url.Error
	if errors.As(err, &ue) {
		return ue.Err
	}

	return err
}

// getJSON performs a GET and decodes a 200 JSON body into out.
// Transport errors, non-200 statuses and undecodable bodies are returned
// wrapped and never as ErrInvalidToken.
func getJSON(ctx context.Context, hc *http.Client, u string, out any) (http.Header, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return nil, fmt.Errorf("build request: %w", stripURL(err))
	}

	resp, err := hc.Do(req)
	if err != nil {
		return nil, fmt.Errorf("provider request to %s: %w", req.URL.Host, stripURL(err))
	}
	defer func() { _ = resp.Body.Close() }()

	if resp.StatusCode != http.StatusOK {
		_, _ = io.Copy(io.Discard, io.LimitReader(resp.Body, 1<<16))
		return nil, &statusError{resp.StatusCode}
	}

	if err := json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(out); err != nil {
		return nil, fmt.Errorf("decode provider response: %w", err)
	}

	return resp.Header, nil
}

// ---- Google ----

// Google verifies Google ID tokens (RS256) against the JWKS endpoint.
type Google struct {
	JWKSURL   string
	ClientIDs []string
	HTTP      *http.Client
	// MinRefetch limits forced refetches triggered by an unknown kid.
	MinRefetch time.Duration

	mu      sync.Mutex
	keys    map[string]*rsa.PublicKey
	expires time.Time
	fetched time.Time
}

type fetchError struct{ err error }

func (e *fetchError) Error() string { return e.err.Error() }
func (e *fetchError) Unwrap() error { return e.err }

func (g *Google) Verify(ctx context.Context, token string) (string, error) {
	var claims jwt.RegisteredClaims

	_, err := jwt.ParseWithClaims(token, &claims, func(t *jwt.Token) (any, error) {
		kid, _ := t.Header["kid"].(string)

		k, err := g.key(ctx, kid)
		if err != nil {
			return nil, &fetchError{err}
		}

		if k == nil {
			return nil, errors.New("unknown kid")
		}

		return k, nil
	}, jwt.WithValidMethods([]string{"RS256"}), jwt.WithExpirationRequired())
	if err != nil {
		var fe *fetchError
		if errors.As(err, &fe) {
			return "", fmt.Errorf("google jwks: %w", fe.err)
		}

		return "", fmt.Errorf("%w: %v", ErrInvalidToken, err)
	}

	if claims.Issuer != "accounts.google.com" && claims.Issuer != "https://accounts.google.com" {
		return "", fmt.Errorf("%w: issuer", ErrInvalidToken)
	}

	if !slices.ContainsFunc(claims.Audience, func(a string) bool { return slices.Contains(g.ClientIDs, a) }) {
		return "", fmt.Errorf("%w: audience", ErrInvalidToken)
	}

	if claims.Subject == "" {
		return "", fmt.Errorf("%w: no subject", ErrInvalidToken)
	}

	return claims.Subject, nil
}

// key returns the cached key for kid, refetching when the cache expired or
// the kid is unknown. A nil key with nil error means the kid does not exist.
func (g *Google) key(ctx context.Context, kid string) (*rsa.PublicKey, error) {
	g.mu.Lock()
	defer g.mu.Unlock()

	now := time.Now()
	if g.keys == nil || !now.Before(g.expires) {
		if err := g.fetch(ctx, now); err != nil {
			return nil, err
		}
	} else if g.keys[kid] == nil && now.Sub(g.fetched) >= g.MinRefetch {
		if err := g.fetch(ctx, now); err != nil {
			return nil, err
		}
	}

	return g.keys[kid], nil
}

func (g *Google) fetch(ctx context.Context, now time.Time) error {
	var doc struct {
		Keys []struct {
			Kid, Kty, N, E string
		} `json:"keys"`
	}

	hdr, err := getJSON(ctx, g.HTTP, g.JWKSURL, &doc)
	if err != nil {
		return err
	}

	keys := map[string]*rsa.PublicKey{}

	for _, k := range doc.Keys {
		if k.Kty != "RSA" {
			continue
		}

		n, err1 := base64.RawURLEncoding.DecodeString(k.N)

		e, err2 := base64.RawURLEncoding.DecodeString(k.E)
		if err1 != nil || err2 != nil {
			continue
		}

		keys[k.Kid] = &rsa.PublicKey{N: new(big.Int).SetBytes(n), E: int(new(big.Int).SetBytes(e).Int64())}
	}

	g.keys, g.fetched = keys, now
	g.expires = now.Add(maxAge(hdr.Get("Cache-Control")))

	return nil
}

func maxAge(cc string) time.Duration {
	for _, p := range strings.Split(cc, ",") {
		p = strings.TrimSpace(p)
		if v, ok := strings.CutPrefix(p, "max-age="); ok {
			if n, err := strconv.Atoi(v); err == nil && n > 0 {
				return time.Duration(n) * time.Second
			}
		}
	}

	return 0
}

// ---- VK ----

type VK struct {
	BaseURL      string
	ServiceToken string
	HTTP         *http.Client
}

func (v *VK) Verify(ctx context.Context, token string) (string, error) {
	q := url.Values{"token": {token}, "access_token": {v.ServiceToken}, "v": {"5.199"}}

	var r struct {
		Response *struct {
			Success int         `json:"success"`
			UserID  json.Number `json:"user_id"`
		} `json:"response"`
	}
	if _, err := getJSON(ctx, v.HTTP, v.BaseURL+"?"+q.Encode(), &r); err != nil {
		return "", rejection(err)
	}

	if r.Response == nil || r.Response.Success != 1 || r.Response.UserID == "" {
		return "", ErrInvalidToken
	}

	return r.Response.UserID.String(), nil
}

// ---- Odnoklassniki ----

type OK struct {
	BaseURL   string
	AppKey    string
	AppSecret string
	HTTP      *http.Client
}

func md5hex(s string) string {
	h := md5.Sum([]byte(s))
	return hex.EncodeToString(h[:])
}

func (o *OK) Verify(ctx context.Context, token string) (string, error) {
	params := map[string]string{
		"method":          "users.getCurrentUser",
		"application_key": o.AppKey,
		"format":          "json",
	}

	names := make([]string, 0, len(params))
	for k := range params {
		names = append(names, k)
	}

	sort.Strings(names)

	var sb strings.Builder
	for _, k := range names {
		sb.WriteString(k + "=" + params[k])
	}

	sig := md5hex(sb.String() + md5hex(token+o.AppSecret))

	q := url.Values{"access_token": {token}, "sig": {sig}}
	for k, v := range params {
		q.Set(k, v)
	}

	var r struct {
		UID       json.Number `json:"uid"`
		ErrorCode int         `json:"error_code"`
	}
	if _, err := getJSON(ctx, o.HTTP, o.BaseURL+"?"+q.Encode(), &r); err != nil {
		return "", rejection(err)
	}

	if r.ErrorCode != 0 || r.UID == "" {
		return "", ErrInvalidToken
	}

	return r.UID.String(), nil
}

// ---- Facebook ----

type FB struct {
	BaseURL   string
	AppID     string
	AppSecret string
	HTTP      *http.Client
}

func (f *FB) Verify(ctx context.Context, token string) (string, error) {
	q := url.Values{"input_token": {token}, "access_token": {f.AppID + "|" + f.AppSecret}}

	var r struct {
		Data *struct {
			IsValid bool        `json:"is_valid"`
			AppID   string      `json:"app_id"`
			UserID  json.Number `json:"user_id"`
		} `json:"data"`
	}
	if _, err := getJSON(ctx, f.HTTP, f.BaseURL+"?"+q.Encode(), &r); err != nil {
		return "", rejection(err)
	}

	if r.Data == nil || !r.Data.IsValid || r.Data.AppID != f.AppID || r.Data.UserID == "" {
		return "", ErrInvalidToken
	}

	return r.Data.UserID.String(), nil
}

// Package fcm sends push notifications through the FCM HTTP v1 API.
package fcm

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strings"

	"golang.org/x/oauth2"
	"golang.org/x/oauth2/google"
)

const (
	defaultBaseURL = "https://fcm.googleapis.com"
	messagingScope = "https://www.googleapis.com/auth/firebase.messaging"
	maxErrorBody   = 64 << 10
)

// ErrUnregistered means the device token is no longer valid and should be dropped.
var ErrUnregistered = errors.New("fcm token unregistered")

// Sender delivers a data push to a single device token.
type Sender interface {
	Send(ctx context.Context, token string, data map[string]string) error
}

// Nop is a Sender that does nothing; used when FCM credentials are not configured.
type Nop struct{}

// Send implements Sender.
func (Nop) Send(context.Context, string, map[string]string) error { return nil }

// HTTPSender is a Sender backed by the FCM HTTP v1 API.
type HTTPSender struct {
	// BaseURL is the FCM endpoint origin, overridable in tests.
	BaseURL string
	// TokenSource provides OAuth2 access tokens; tests replace it.
	TokenSource oauth2.TokenSource

	projectID string
	hc        *http.Client
}

// NewHTTP builds a sender from service-account credentials JSON; project_id is read from it.
func NewHTTP(credentialsJSON []byte, hc *http.Client) (*HTTPSender, error) {
	var meta struct {
		ProjectID string `json:"project_id"`
	}

	if err := json.Unmarshal(credentialsJSON, &meta); err != nil {
		return nil, fmt.Errorf("parse fcm credentials: %w", err)
	}

	if meta.ProjectID == "" {
		return nil, errors.New("fcm credentials: project_id is missing")
	}

	if hc == nil {
		hc = http.DefaultClient
	}

	ctx := context.WithValue(context.Background(), oauth2.HTTPClient, hc)

	creds, err := google.CredentialsFromJSONWithType(ctx, credentialsJSON, google.ServiceAccount, messagingScope)
	if err != nil {
		return nil, fmt.Errorf("fcm credentials: %w", err)
	}

	return &HTTPSender{
		BaseURL:     defaultBaseURL,
		TokenSource: creds.TokenSource,
		projectID:   meta.ProjectID,
		hc:          hc,
	}, nil
}

type androidConfig struct {
	Priority string `json:"priority"`
	TTL      string `json:"ttl"`
}

type message struct {
	Token   string            `json:"token"`
	Data    map[string]string `json:"data"`
	Android androidConfig     `json:"android"`
}

// Send pushes data to token. It returns ErrUnregistered when FCM reports the token as gone.
func (s *HTTPSender) Send(ctx context.Context, token string, data map[string]string) error {
	body, err := json.Marshal(map[string]message{"message": {
		Token:   token,
		Data:    data,
		Android: androidConfig{Priority: "HIGH", TTL: "0s"},
	}})
	if err != nil {
		return fmt.Errorf("encode fcm message: %w", err)
	}

	req, err := s.newRequest(ctx, body)
	if err != nil {
		return err
	}

	resp, err := s.hc.Do(req)
	if err != nil {
		// url.Error carries only the endpoint URL, never the token or headers.
		return fmt.Errorf("fcm request: %w", err)
	}
	defer func() { _ = resp.Body.Close() }()

	return checkResponse(resp)
}

func (s *HTTPSender) newRequest(ctx context.Context, body []byte) (*http.Request, error) {
	tok, err := s.TokenSource.Token()
	if err != nil {
		return nil, errors.New("fcm: obtain access token failed")
	}

	url := fmt.Sprintf("%s/v1/projects/%s/messages:send", s.BaseURL, s.projectID)

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, bytes.NewReader(body))
	if err != nil {
		return nil, fmt.Errorf("build fcm request: %w", err)
	}

	tok.SetAuthHeader(req)
	req.Header.Set("Content-Type", "application/json")

	return req, nil
}

func checkResponse(resp *http.Response) error {
	if resp.StatusCode >= 200 && resp.StatusCode < 300 {
		return nil
	}

	raw, _ := io.ReadAll(io.LimitReader(resp.Body, maxErrorBody))

	if resp.StatusCode == http.StatusNotFound || isUnregistered(raw) {
		return ErrUnregistered
	}

	return fmt.Errorf("fcm: unexpected status %d", resp.StatusCode)
}

func isUnregistered(raw []byte) bool {
	compact := strings.Join(strings.Fields(string(raw)), "")

	return strings.Contains(compact, `"errorCode":"UNREGISTERED"`)
}

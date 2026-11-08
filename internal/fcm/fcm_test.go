package fcm

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"golang.org/x/oauth2"
)

const testCredentials = `{"type":"service_account","project_id":"demo-project"}`

func newTestSender(t *testing.T, handler http.HandlerFunc) *HTTPSender {
	t.Helper()

	srv := httptest.NewServer(handler)
	t.Cleanup(srv.Close)

	s, err := NewHTTP([]byte(testCredentials), srv.Client())
	if err != nil {
		t.Fatalf("NewHTTP: %v", err)
	}

	s.BaseURL = srv.URL
	s.TokenSource = oauth2.StaticTokenSource(&oauth2.Token{AccessToken: "access-123"})

	return s
}

func TestSendBuildsMessage(t *testing.T) {
	var (
		path string
		auth string
		body struct {
			Message struct {
				Token   string            `json:"token"`
				Data    map[string]string `json:"data"`
				Android struct {
					Priority string `json:"priority"`
					TTL      string `json:"ttl"`
				} `json:"android"`
			} `json:"message"`
		}
	)

	s := newTestSender(t, func(w http.ResponseWriter, r *http.Request) {
		path = r.URL.Path
		auth = r.Header.Get("Authorization")
		raw, _ := io.ReadAll(r.Body)
		_ = json.Unmarshal(raw, &body)
		_, _ = w.Write([]byte(`{"name":"projects/demo-project/messages/1"}`))
	})

	err := s.Send(context.Background(), "dev-token", map[string]string{"type": "invite"})
	if err != nil {
		t.Fatalf("Send: %v", err)
	}

	if path != "/v1/projects/demo-project/messages:send" {
		t.Errorf("path = %q", path)
	}

	if auth != "Bearer access-123" {
		t.Errorf("Authorization = %q", auth)
	}

	if body.Message.Token != "dev-token" || body.Message.Data["type"] != "invite" {
		t.Errorf("message = %+v", body.Message)
	}

	if body.Message.Android.Priority != "HIGH" || body.Message.Android.TTL != "0s" {
		t.Errorf("android = %+v", body.Message.Android)
	}
}

func TestSendUnregistered(t *testing.T) {
	s := newTestSender(t, func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusNotFound)
	})

	err := s.Send(context.Background(), "dev-token", map[string]string{"type": "invite"})
	if !errors.Is(err, ErrUnregistered) {
		t.Fatalf("err = %v, want ErrUnregistered", err)
	}
}

func TestSendUnregisteredByErrorCode(t *testing.T) {
	s := newTestSender(t, func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte(`{"error":{"details":[{"errorCode":"UNREGISTERED"}]}}`))
	})

	err := s.Send(context.Background(), "dev-token", nil)
	if !errors.Is(err, ErrUnregistered) {
		t.Fatalf("err = %v, want ErrUnregistered", err)
	}
}

func TestSendOtherErrorHidesSecrets(t *testing.T) {
	s := newTestSender(t, func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
	})

	err := s.Send(context.Background(), "dev-token", nil)
	if err == nil || errors.Is(err, ErrUnregistered) {
		t.Fatalf("err = %v", err)
	}

	if !strings.Contains(err.Error(), "500") {
		t.Errorf("no status in %q", err)
	}

	if strings.Contains(err.Error(), "dev-token") || strings.Contains(err.Error(), "access-123") {
		t.Errorf("secret leaked: %q", err)
	}
}

func TestSendHonorsContext(t *testing.T) {
	s := newTestSender(t, func(http.ResponseWriter, *http.Request) {})
	ctx, cancel := context.WithCancel(context.Background())
	cancel()

	if err := s.Send(ctx, "dev-token", nil); err == nil {
		t.Fatal("expected error for cancelled context")
	}
}

func TestNewHTTPRequiresProjectID(t *testing.T) {
	if _, err := NewHTTP([]byte(`{"type":"service_account"}`), nil); err == nil {
		t.Fatal("expected error for missing project_id")
	}
}

func TestNopSend(t *testing.T) {
	var s Sender = Nop{}

	if err := s.Send(context.Background(), "t", nil); err != nil {
		t.Fatal(err)
	}
}

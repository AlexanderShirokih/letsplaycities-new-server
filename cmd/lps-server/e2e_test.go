package main

import (
	"bytes"
	"context"
	"encoding/json"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/coder/websocket"

	"lps/internal/config"
	"lps/internal/game"
)

const ioTimeout = 5 * time.Second

func testConfig(t *testing.T, withDict bool) config.Config {
	t.Helper()

	dir := t.TempDir()

	if withDict {
		data, err := os.ReadFile(filepath.Join("..", "..", "testdata", "dict", "data-80.db2"))
		if err != nil {
			t.Fatal(err)
		}

		if err := os.WriteFile(filepath.Join(dir, "data-80.db2"), data, 0o600); err != nil {
			t.Fatal(err)
		}
	}

	return config.Config{
		DataDir:      dir,
		NativePepper: []byte("pepper"),
		MoveTime:     92 * time.Second,
		PingInterval: 5 * time.Second,
		PingTimeout:  15 * time.Second,
		InviteTTL:    60 * time.Second,
		BotsWait:     15 * time.Second,
	}
}

// startServer runs the server on a loopback listener with the real clock
// (the test finishes far inside the move time) and stops it on cleanup.
func startServer(t *testing.T, cfg config.Config) string {
	t.Helper()

	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}

	ctx, cancel := context.WithCancel(context.Background())
	done := make(chan error, 1)

	go func() { done <- run(ctx, cfg, ln, game.RealClock{}) }()

	t.Cleanup(func() {
		cancel()

		select {
		case err := <-done:
			if err != nil {
				t.Errorf("run: %v", err)
			}
		case <-time.After(ioTimeout):
			t.Error("run did not stop")
		}
	})

	return ln.Addr().String()
}

type client struct {
	t     *testing.T
	base  string
	id    int64
	token string
	ws    *websocket.Conn
}

func (c *client) request(method, path string, body any) map[string]any {
	c.t.Helper()

	var rd bytes.Buffer

	if body != nil {
		if err := json.NewEncoder(&rd).Encode(body); err != nil {
			c.t.Fatal(err)
		}
	}

	req, err := http.NewRequest(method, "http://"+c.base+path, &rd)
	if err != nil {
		c.t.Fatal(err)
	}

	if c.token != "" {
		req.Header.Set("Authorization", "Bearer "+c.token)
	}

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		c.t.Fatal(err)
	}
	defer func() { _ = resp.Body.Close() }()

	var out map[string]any

	_ = json.NewDecoder(resp.Body).Decode(&out)

	if resp.StatusCode >= 300 {
		c.t.Fatalf("%s %s: status %d: %v", method, path, resp.StatusCode, out)
	}

	return out
}

func login(t *testing.T, base, device, name string) *client {
	t.Helper()

	c := &client{t: t, base: base}
	out := c.request("POST", "/v1/auth/native", map[string]string{"deviceId": device, "name": name})
	c.token, _ = out["token"].(string)
	user, _ := out["user"].(map[string]any)
	id, _ := user["id"].(float64)
	c.id = int64(id)

	if c.token == "" || c.id == 0 {
		t.Fatalf("bad login response: %v", out)
	}

	return c
}

func (c *client) dial() {
	c.t.Helper()

	ctx, cancel := context.WithTimeout(context.Background(), ioTimeout)
	defer cancel()

	hdr := http.Header{"Authorization": {"Bearer " + c.token}}

	ws, _, err := websocket.Dial(ctx, "ws://"+c.base+"/v1/game", &websocket.DialOptions{HTTPHeader: hdr})
	if err != nil {
		c.t.Fatalf("dial: %v", err)
	}

	c.t.Cleanup(func() { _ = ws.CloseNow() })

	c.ws = ws
}

func (c *client) send(msg string) {
	c.t.Helper()

	ctx, cancel := context.WithTimeout(context.Background(), ioTimeout)
	defer cancel()

	if err := c.ws.Write(ctx, websocket.MessageText, []byte(msg)); err != nil {
		c.t.Fatalf("write %s: %v", msg, err)
	}
}

// expect reads messages until one of the given type arrives.
func (c *client) expect(typ string) map[string]any {
	c.t.Helper()

	for range 10 {
		ctx, cancel := context.WithTimeout(context.Background(), ioTimeout)
		_, data, err := c.ws.Read(ctx)

		cancel()

		if err != nil {
			c.t.Fatalf("waiting for %s: %v", typ, err)
		}

		var m map[string]any
		if err := json.Unmarshal(data, &m); err != nil {
			c.t.Fatalf("decode %s: %v", data, err)
		}

		if m["type"] == typ {
			return m
		}
	}

	c.t.Fatalf("no %s message", typ)

	return nil
}

func TestEndToEnd(t *testing.T) {
	base := startServer(t, testConfig(t, true))
	a := login(t, base, "device-a", "Alice")
	b := login(t, base, "device-b", "Bob")

	a.request("PUT", "/v1/friends/"+itoa(b.id), nil)
	b.request("PUT", "/v1/friends/"+itoa(a.id), nil)
	b.request("PATCH", "/v1/me", map[string]string{"fcmToken": "token-b"}) // invites are delivered by push

	for _, c := range []*client{a, b} {
		c.dial()
		c.send(`{"type":"hello","protocol":1,"client":"e2e"}`)
		c.expect("welcome")
	}

	a.send(`{"type":"invite","userId":` + itoa(b.id) + `}`)

	if m := a.expect("inviteStatus"); m["status"] != "sent" {
		t.Fatalf("invite not sent: %v", m)
	}

	b.send(`{"type":"inviteReply","userId":` + itoa(a.id) + `,"accept":true}`)

	if m := a.expect("match"); m["youStart"] != true {
		t.Fatalf("inviter must start: %v", m)
	}

	b.expect("match")

	a.send(`{"type":"word","text":"москва"}`)
	a.expect("word")
	b.expect("word")
	b.send(`{"type":"word","text":"анапа"}`)

	if m := a.expect("word"); m["by"] != float64(b.id) || m["status"] != "accepted" {
		t.Fatalf("unexpected echo: %v", m)
	}

	a.send(`{"type":"leave"}`)

	over := b.expect("gameOver")
	if over["reason"] != "surrender" || over["loser"] != float64(a.id) {
		t.Fatalf("unexpected gameOver: %v", over)
	}

	assertHistory(t, b, a.id)
}

func assertHistory(t *testing.T, b *client, opponent int64) {
	t.Helper()

	out := b.request("GET", "/v1/history", nil)
	games, _ := out["games"].([]any)

	if len(games) != 1 {
		t.Fatalf("want 1 game in history, got %v", out)
	}

	g, _ := games[0].(map[string]any)
	opp, _ := g["opponent"].(map[string]any)

	if g["words"] != float64(2) || g["isFriend"] != true || opp["id"] != float64(opponent) {
		t.Fatalf("unexpected history entry: %v", g)
	}
}

func itoa(n int64) string { return strconv.FormatInt(n, 10) }

func TestNoDictionary(t *testing.T) {
	cfg := testConfig(t, false)

	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}

	defer func() { _ = ln.Close() }()

	err = run(context.Background(), cfg, ln, game.RealClock{})
	if err == nil || !strings.Contains(err.Error(), "no dictionary") {
		t.Fatalf("want no dictionary error, got %v", err)
	}
}

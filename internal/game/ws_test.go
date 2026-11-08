package game

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/coder/websocket"

	"lps/internal/store"
)

const wsTimeout = time.Second

var errTestUnauthorized = http.ErrNoCookie

type wsFixture struct {
	t   *testing.T
	f   *hubFixture
	srv *httptest.Server
}

func newWSFixture(t *testing.T, cfg WSConfig) *wsFixture {
	t.Helper()

	f := newHubFixture(t)
	ctx, cancel := context.WithCancel(context.Background())
	done := make(chan struct{})

	go func() {
		defer close(done)

		f.hub.Run(ctx)
	}()

	authn := func(r *http.Request) (store.User, error) {
		tok := strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer ")

		for _, u := range f.st.users {
			if tok == "token-"+u.Name {
				return u, nil
			}
		}

		return store.User{}, errTestUnauthorized
	}
	authErr := func(w http.ResponseWriter, _ error) {
		http.Error(w, `{"error":{"code":"unauthorized"}}`, http.StatusUnauthorized)
	}
	srv := httptest.NewServer(WSHandler(f.hub, authn, authErr, cfg))

	t.Cleanup(func() {
		srv.Close()
		cancel()
		<-done
	})

	return &wsFixture{t: t, f: f, srv: srv}
}

func (w *wsFixture) dial(token string) (*websocket.Conn, *http.Response, error) {
	w.t.Helper()

	ctx, cancel := context.WithTimeout(context.Background(), wsTimeout)
	defer cancel()

	url := "ws" + strings.TrimPrefix(w.srv.URL, "http")
	hdr := http.Header{}

	if token != "" {
		hdr.Set("Authorization", "Bearer "+token)
	}

	return websocket.Dial(ctx, url, &websocket.DialOptions{HTTPHeader: hdr})
}

func (w *wsFixture) connect(name string) *websocket.Conn {
	w.t.Helper()

	c, _, err := w.dial("token-" + name)
	if err != nil {
		w.t.Fatalf("dial %s: %v", name, err)
	}

	w.t.Cleanup(func() { _ = c.CloseNow() })

	return c
}

func wsSend(t *testing.T, c *websocket.Conn, s string) {
	t.Helper()

	ctx, cancel := context.WithTimeout(context.Background(), wsTimeout)
	defer cancel()

	if err := c.Write(ctx, websocket.MessageText, []byte(s)); err != nil {
		t.Fatalf("write %s: %v", s, err)
	}
}

func wsRead(t *testing.T, c *websocket.Conn) map[string]any {
	t.Helper()

	ctx, cancel := context.WithTimeout(context.Background(), 2*wsTimeout)
	defer cancel()

	_, data, err := c.Read(ctx)
	if err != nil {
		t.Fatalf("read: %v", err)
	}

	var m map[string]any
	if err := json.Unmarshal(data, &m); err != nil {
		t.Fatalf("decode %s: %v", data, err)
	}

	return m
}

// wsReadType reads messages until one of the given type arrives.
func wsReadType(t *testing.T, c *websocket.Conn, typ string) map[string]any {
	t.Helper()

	for range 5 {
		if m := wsRead(t, c); m["type"] == typ {
			return m
		}
	}

	t.Fatalf("no %s message", typ)

	return nil
}

const helloMsg = `{"type":"hello","protocol":1,"client":"test"}`

func TestWSRejectsNoToken(t *testing.T) {
	w := newWSFixture(t, WSConfig{PingInterval: time.Second, PingTimeout: time.Second})

	_, resp, err := w.dial("")
	if err == nil {
		t.Fatal("dial without token succeeded")
	}

	if resp == nil || resp.StatusCode != http.StatusUnauthorized {
		t.Fatalf("response = %v, want 401", resp)
	}
}

func TestWSHelloWelcome(t *testing.T) {
	w := newWSFixture(t, WSConfig{PingInterval: time.Second, PingTimeout: time.Second})
	c := w.connect("A")

	wsSend(t, c, helloMsg)

	got := wsRead(t, c)
	if got["type"] != TypeWelcome {
		t.Fatalf("got %v, want welcome", got)
	}

	user, _ := got["user"].(map[string]any)
	if user["id"] != float64(userA) {
		t.Fatalf("welcome user = %v, want id %d", user, userA)
	}
}

func TestWSBadMessage(t *testing.T) {
	w := newWSFixture(t, WSConfig{PingInterval: time.Second, PingTimeout: time.Second})
	c := w.connect("A")

	wsSend(t, c, "{")

	got := wsRead(t, c)
	if got["type"] != TypeError || got["code"] != ErrCodeBadMessage {
		t.Fatalf("got %v, want error bad_message", got)
	}

	wsSend(t, c, helloMsg)

	if got := wsRead(t, c); got["type"] != TypeWelcome {
		t.Fatalf("got %v, want welcome after bad message", got)
	}
}

func TestWSPingTimeout(t *testing.T) {
	w := newWSFixture(t, WSConfig{PingInterval: 50 * time.Millisecond, PingTimeout: 100 * time.Millisecond})
	a, b := w.connect("A"), w.connect("B")

	for _, c := range []*websocket.Conn{a, b} {
		wsSend(t, c, helloMsg)
		wsRead(t, c)
		wsSend(t, c, `{"type":"find"}`)
	}

	wsReadType(t, b, TypeMatch)

	// a never reads again, so its pongs stop: the server must drop it and
	// the hub ends the game for b.
	start := time.Now()

	got := wsRead(t, b)
	if got["type"] != TypeGameOver || got["reason"] != ReasonOpponentLeft {
		t.Fatalf("b got %v, want gameOver opponentLeft", got)
	}

	if d := time.Since(start); d > wsTimeout {
		t.Fatalf("disconnect took %v", d)
	}
}

func TestWSTwoPlayersMatch(t *testing.T) {
	w := newWSFixture(t, WSConfig{PingInterval: time.Second, PingTimeout: time.Second})
	a, b := w.connect("A"), w.connect("B")

	for _, c := range []*websocket.Conn{a, b} {
		wsSend(t, c, helloMsg)
		wsRead(t, c)
		wsSend(t, c, `{"type":"find"}`)
	}

	for _, c := range []*websocket.Conn{a, b} {
		wsReadType(t, c, TypeMatch)
	}
}

func TestWSReplacedConnection(t *testing.T) {
	w := newWSFixture(t, WSConfig{PingInterval: time.Second, PingTimeout: time.Second})
	old := w.connect("A")

	wsSend(t, old, helloMsg)
	wsRead(t, old)

	fresh := w.connect("A")

	wsSend(t, fresh, helloMsg)

	if got := wsRead(t, fresh); got["type"] != TypeWelcome {
		t.Fatalf("fresh got %v, want welcome", got)
	}

	got := wsRead(t, old)
	if got["type"] != TypeError || got["code"] != ErrCodeReplaced {
		t.Fatalf("old got %v, want error replaced", got)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 2*wsTimeout)
	defer cancel()

	if _, _, err := old.Read(ctx); websocket.CloseStatus(err) != websocket.StatusGoingAway {
		t.Fatalf("old read err = %v, want going away close", err)
	}
}

func TestWSConnSendOverflowCloses(t *testing.T) {
	c := newWSConn(nil, WSConfig{})

	for range wsSendBuffer {
		c.Send([]byte("x"))
	}

	if c.isClosing() {
		t.Fatal("closed before the buffer is full")
	}

	c.Send([]byte("x"))

	if !c.isClosing() {
		t.Fatal("not closed after the buffer overflowed")
	}

	c.Send([]byte("x"))

	if len(c.out) != wsSendBuffer {
		t.Fatalf("queued %d after close, want %d", len(c.out), wsSendBuffer)
	}
}

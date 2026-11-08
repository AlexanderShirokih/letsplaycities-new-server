package game

import (
	"context"
	"net/http"
	"sync"
	"time"

	"github.com/coder/websocket"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

const (
	wsSendBuffer = 64
	wsReadLimit  = 64 << 10

	codeGone      = "gone"
	codeOverload  = "overloaded"
	codeNotText   = "unsupported_frame"
	maxCloseWrite = 5 * time.Second
)

// WSConfig holds the WebSocket keep-alive timings.
type WSConfig struct {
	PingInterval, PingTimeout time.Duration
}

// WSHandler upgrades authenticated requests to WebSocket and attaches them
// to the hub. authn runs before the upgrade; its failure is reported with
// authErr as an ordinary HTTP response.
func WSHandler(
	h *Hub,
	authn func(*http.Request) (store.User, error),
	authErr func(http.ResponseWriter, error),
	cfg WSConfig,
) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		u, err := authn(r)
		if err != nil {
			authErr(w, err)

			return
		}

		// Mobile clients send no browser cookies, so the Origin is irrelevant.
		ws, err := websocket.Accept(w, r, &websocket.AcceptOptions{OriginPatterns: []string{"*"}})
		if err != nil {
			return // Accept has already written the HTTP error.
		}

		ws.SetReadLimit(wsReadLimit)
		serveWS(r.Context(), h, u, ws, cfg)
	})
}

func serveWS(ctx context.Context, h *Hub, u store.User, ws *websocket.Conn, cfg WSConfig) {
	c := newWSConn(ws, cfg)
	writerDone := make(chan struct{})

	go func() {
		defer close(writerDone)

		c.writeLoop(ctx)
	}()

	h.Connect(u, c)
	c.readLoop(ctx, h, u.ID)

	c.Close(codeGone)
	<-writerDone

	h.Disconnect(u.ID, c)
}

// wsConn is a hub Conn backed by a WebSocket. Send and Close never block:
// a single writer goroutine owns every write on the socket.
type wsConn struct {
	ws  *websocket.Conn
	cfg WSConfig
	out chan []byte

	once    sync.Once
	closing chan struct{}
	code    string // set before closing is closed
}

func newWSConn(ws *websocket.Conn, cfg WSConfig) *wsConn {
	return &wsConn{
		ws:      ws,
		cfg:     cfg,
		out:     make(chan []byte, wsSendBuffer),
		closing: make(chan struct{}),
	}
}

// Send queues msg for the writer. A connection that cannot keep up is closed.
func (c *wsConn) Send(msg []byte) {
	if c.isClosing() {
		return
	}

	select {
	case c.out <- msg:
	default:
		c.Close(codeOverload)
	}
}

// Close asks the writer to flush queued messages and send a close frame.
// Only the first call has an effect.
func (c *wsConn) Close(code string) {
	c.once.Do(func() {
		c.code = code

		close(c.closing)
	})
}

func (c *wsConn) isClosing() bool {
	select {
	case <-c.closing:
		return true
	default:
		return false
	}
}

func (c *wsConn) readLoop(ctx context.Context, h *Hub, userID int64) {
	for {
		typ, data, err := c.ws.Read(ctx)
		if err != nil {
			return
		}

		// Frames that arrive after Close must not reach the hub: the user
		// may already be served by another connection.
		if c.isClosing() {
			return
		}

		if typ != websocket.MessageText {
			c.Close(codeNotText)

			return
		}

		msg, err := DecodeClient(data)
		if err != nil {
			c.Send(Error(ErrCodeBadMessage))

			continue
		}

		h.Message(userID, c, msg)
	}
}

func (c *wsConn) writeLoop(ctx context.Context) {
	defer c.Close(codeGone)

	var tick <-chan time.Time

	if c.cfg.PingInterval > 0 {
		t := time.NewTicker(c.cfg.PingInterval)
		defer t.Stop()

		tick = t.C
	}

	for {
		select {
		case msg := <-c.out:
			if !c.write(ctx, msg) {
				return
			}
		case <-tick:
			if !c.ping(ctx) {
				return
			}
		case <-c.closing:
			c.finish(ctx)

			return
		}
	}
}

func (c *wsConn) write(ctx context.Context, msg []byte) bool {
	wctx, cancel := context.WithTimeout(ctx, c.writeTimeout())
	defer cancel()

	if err := c.ws.Write(wctx, websocket.MessageText, msg); err != nil {
		_ = c.ws.CloseNow()

		return false
	}

	return true
}

func (c *wsConn) ping(ctx context.Context) bool {
	pctx, cancel := context.WithTimeout(ctx, c.writeTimeout())
	defer cancel()

	if err := c.ws.Ping(pctx); err != nil {
		_ = c.ws.CloseNow()

		return false
	}

	return true
}

// finish flushes queued messages and sends the close frame.
func (c *wsConn) finish(ctx context.Context) {
	for {
		select {
		case msg := <-c.out:
			if !c.write(ctx, msg) {
				return
			}

			continue
		default:
		}

		break
	}

	status, reason := closeStatus(c.code)
	_ = c.ws.Close(status, reason)
}

func (c *wsConn) writeTimeout() time.Duration {
	if c.cfg.PingTimeout > 0 {
		return c.cfg.PingTimeout
	}

	return maxCloseWrite
}

func closeStatus(code string) (websocket.StatusCode, string) {
	switch code {
	case ErrCodeReplaced:
		return websocket.StatusGoingAway, code
	case codeGone:
		return websocket.StatusNormalClosure, ""
	case codeNotText:
		return websocket.StatusUnsupportedData, code
	default:
		return websocket.StatusPolicyViolation, code
	}
}

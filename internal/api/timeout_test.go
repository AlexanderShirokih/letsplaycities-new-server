package api

import (
	"bufio"
	"fmt"
	"net"
	"net/http"
	"testing"
	"time"
)

func TestSlowBodyTimesOut(t *testing.T) {
	old := bodyReadTimeout
	bodyReadTimeout = 100 * time.Millisecond

	t.Cleanup(func() { bodyReadTimeout = old })

	e := newTestServer(t)
	a := e.native("device-slow", "Slow")

	for _, tc := range []struct {
		method, path, ctype string
		want                int
	}{
		{"POST", "/v1/auth/native", "application/json", http.StatusBadRequest},
		{"PUT", "/v1/me/avatar", "image/png", http.StatusBadRequest},
		{"POST", "/v1/unknown", "application/json", http.StatusNotFound},
		{"POST", "/v1/auth/logout", "application/json", http.StatusNoContent},
	} {
		t.Run(tc.path, func(t *testing.T) {
			conn, err := net.Dial("tcp", e.ts.Listener.Addr().String())
			if err != nil {
				t.Fatal(err)
			}

			defer func() { _ = conn.Close() }()

			_, _ = fmt.Fprintf(conn, "%s %s HTTP/1.1\r\nHost: test\r\nAuthorization: Bearer %s\r\n"+
				"Content-Type: %s\r\nContent-Length: 100\r\n\r\n{", tc.method, tc.path, a.Token, tc.ctype)
			_ = conn.SetReadDeadline(time.Now().Add(2 * time.Second))

			resp, err := http.ReadResponse(bufio.NewReader(conn), nil)
			if err != nil {
				t.Fatalf("no answer to a stalled body: %v", err)
			}

			_ = resp.Body.Close()

			if resp.StatusCode != tc.want {
				t.Fatalf("status %d, want %d", resp.StatusCode, tc.want)
			}
		})
	}
}

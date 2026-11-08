package main

import (
	"net/http"
	"testing"
	"time"
)

func TestHTTPServerTimeouts(t *testing.T) {
	srv := newHTTPServer(http.NotFoundHandler())

	if srv.IdleTimeout != 120*time.Second || srv.ReadHeaderTimeout != headerTimeout {
		t.Fatalf("idle %v, header %v", srv.IdleTimeout, srv.ReadHeaderTimeout)
	}

	// A server-wide read or write timeout would cut every game socket.
	if srv.ReadTimeout != 0 || srv.WriteTimeout != 0 {
		t.Fatalf("read %v, write %v, want none", srv.ReadTimeout, srv.WriteTimeout)
	}
}

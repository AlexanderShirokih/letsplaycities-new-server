package main

import (
	"context"
	"net"
	"net/http"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"lps/internal/auth"
	"lps/internal/game"
	"lps/internal/store"
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

func TestRunRefusesOtherPepper(t *testing.T) {
	cfg := testConfig(t, true)

	st, err := store.Open(filepath.Join(cfg.DataDir, "lps.db"))
	if err != nil {
		t.Fatal(err)
	}

	if err := st.CheckPepper(context.Background(), auth.PepperCheck([]byte("migration-pepper"))); err != nil {
		t.Fatal(err)
	}

	if err := st.Close(); err != nil {
		t.Fatal(err)
	}

	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}

	defer func() { _ = ln.Close() }()

	cfg.NativePepper = []byte("s3cret-xyz")

	ctx, cancel := context.WithTimeout(context.Background(), ioTimeout)
	defer cancel()

	err = run(ctx, cfg, ln, game.RealClock{})
	if err == nil || !strings.Contains(err.Error(), "LPS_NATIVE_PEPPER") {
		t.Fatalf("want pepper mismatch error, got %v", err)
	}

	for _, secret := range []string{"s3cret-xyz", "migration-pepper"} {
		if strings.Contains(err.Error(), secret) || strings.Contains(err.Error(), auth.PepperCheck([]byte(secret))) {
			t.Fatalf("error %q reveals the pepper or its check", err)
		}
	}
}

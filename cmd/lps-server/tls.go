package main

import (
	"context"
	"crypto/tls"
	"errors"
	"fmt"
	"log"
	"net"
	"net/http"
	"path/filepath"
	"time"

	"golang.org/x/crypto/acme/autocert"

	"lps/internal/config"
)

const acmeAddr = ":80"

// listen opens the TLS listener on cfg.Addr according to the TLS mode.
func listen(ctx context.Context, cfg config.Config) (net.Listener, error) {
	var (
		tc  *tls.Config
		err error
	)

	if cfg.TLS() == config.TLSAutocert {
		tc, err = autocertConfig(ctx, cfg)
	} else {
		tc, err = fileConfig(cfg)
	}

	if err != nil {
		return nil, err
	}

	ln, err := net.Listen("tcp", cfg.Addr)
	if err != nil {
		return nil, err
	}

	return tls.NewListener(ln, tc), nil
}

func fileConfig(cfg config.Config) (*tls.Config, error) {
	cert, err := tls.LoadX509KeyPair(cfg.TLSCert, cfg.TLSKey)
	if err != nil {
		return nil, fmt.Errorf("load tls key pair: %w", err)
	}

	return &tls.Config{Certificates: []tls.Certificate{cert}, MinVersion: tls.VersionTLS12}, nil
}

// autocertConfig obtains certificates from Let's Encrypt and serves the
// HTTP-01 challenge on :80 until ctx is done.
func autocertConfig(ctx context.Context, cfg config.Config) (*tls.Config, error) {
	m := &autocert.Manager{
		Prompt:     autocert.AcceptTOS,
		HostPolicy: autocert.HostWhitelist(cfg.Domain),
		Cache:      autocert.DirCache(filepath.Join(cfg.DataDir, "autocert")),
	}

	ln, err := net.Listen("tcp", acmeAddr)
	if err != nil {
		return nil, fmt.Errorf("listen for acme challenge: %w", err)
	}

	srv := &http.Server{Handler: m.HTTPHandler(nil), ReadHeaderTimeout: headerTimeout}

	go func() {
		if err := srv.Serve(ln); !errors.Is(err, http.ErrServerClosed) {
			log.Printf("lps-server: acme http: %v", err)
		}
	}()

	go func() {
		<-ctx.Done()

		sctx, cancel := context.WithTimeout(context.Background(), time.Second)
		defer cancel()

		_ = srv.Shutdown(sctx)
	}()

	tc := m.TLSConfig()
	tc.MinVersion = tls.VersionTLS12

	return tc, nil
}

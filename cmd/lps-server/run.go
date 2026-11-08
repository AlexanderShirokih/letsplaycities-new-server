package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"math/rand"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"time"

	"lps/internal/api"
	"lps/internal/auth"
	"lps/internal/config"
	"lps/internal/dict"
	"lps/internal/fcm"
	"lps/internal/game"
	"lps/internal/store"
)

const (
	httpClientTimeout = 10 * time.Second
	headerTimeout     = 10 * time.Second
	shutdownTimeout   = 10 * time.Second
	idleTimeout       = 120 * time.Second
	authLimit         = 10 // per IP and window, for each of auth/native and auth/social
	authWindow        = time.Minute
)

// run wires the server together and serves on ln until ctx is done, then
// shuts down gracefully.
func run(ctx context.Context, cfg config.Config, ln net.Listener, clk game.Clock) error {
	d, dictPath, err := dict.LoadLatest(cfg.DataDir)
	if err != nil {
		return fmt.Errorf("no dictionary in %s: %w", cfg.DataDir, err)
	}

	st, err := openStore(ctx, cfg)
	if err != nil {
		return err
	}
	defer func() { _ = st.Close() }()

	hc := &http.Client{Timeout: httpClientTimeout}

	push, err := newSender(cfg, hc)
	if err != nil {
		return err
	}

	hub := game.NewHub(hubConfig(cfg), st, d, push, clk)

	nBots, err := addBots(cfg, hub, d, clk)
	if err != nil {
		return err
	}

	log.Printf("lps-server: listening on %s, tls=%s, dictionary v%d, bots=%d", ln.Addr(), tlsMode(cfg), d.Version, nBots)

	hubCtx, stopHub := context.WithCancel(context.Background())
	hubDone := make(chan struct{})

	go func() {
		defer close(hubDone)

		hub.Run(hubCtx)
	}()

	err = serve(ctx, newHTTPServer(newHandler(cfg, st, hc, hub, d, dictPath)), ln)

	stopHub()
	<-hubDone

	return err
}

// openStore opens lps.db and makes sure it was created with this pepper;
// a fresh database takes it.
func openStore(ctx context.Context, cfg config.Config) (*store.Store, error) {
	st, err := store.Open(filepath.Join(cfg.DataDir, "lps.db"))
	if err != nil {
		return nil, err
	}

	if err := st.CheckPepper(ctx, auth.PepperCheck(cfg.NativePepper)); err != nil {
		_ = st.Close()

		return nil, fmt.Errorf("check pepper: %w", err)
	}

	return st, nil
}

// serve runs srv until it fails or ctx is done, in which case it shuts srv down.
func serve(ctx context.Context, srv *http.Server, ln net.Listener) error {
	errc := make(chan error, 1)

	go func() { errc <- srv.Serve(ln) }()

	select {
	case err := <-errc:
		return err
	case <-ctx.Done():
	}

	sctx, cancel := context.WithTimeout(context.Background(), shutdownTimeout)
	defer cancel()

	if err := srv.Shutdown(sctx); err != nil {
		return fmt.Errorf("shutdown: %w", err)
	}

	if err := <-errc; !errors.Is(err, http.ErrServerClosed) {
		return err
	}

	return nil
}

func newHandler(
	cfg config.Config, st *store.Store, hc *http.Client, hub *game.Hub, d *dict.Dictionary, dictPath string,
) http.Handler {
	apiSrv := api.NewServer(api.Deps{
		Store:         st,
		Verifiers:     auth.NewVerifiers(cfg, hc),
		Pepper:        cfg.NativePepper,
		NativeLimiter: auth.NewLimiter(authLimit, authWindow, time.Now),
		SocialLimiter: auth.NewLimiter(authLimit, authWindow, time.Now),
		Dict:          func() *dict.Dictionary { return d },
		DictPath:      func() string { return dictPath },
		Now:           time.Now,
	})
	ws := game.WSHandler(hub, apiSrv.Authenticate, api.WriteAuthError, game.WSConfig{
		PingInterval: cfg.PingInterval,
		PingTimeout:  cfg.PingTimeout,
	})

	// The more specific pattern wins over the API's catch-all.
	mux := http.NewServeMux()
	mux.Handle("/", apiSrv.Handler())
	mux.Handle("GET /v1/game", ws)

	return mux
}

func hubConfig(cfg config.Config) game.HubConfig {
	return game.HubConfig{
		MoveTime:    cfg.MoveTime,
		InviteTTL:   cfg.InviteTTL,
		BotsWait:    cfg.BotsWait,
		BotsEnabled: cfg.BotsEnabled,
	}
}

// newSender returns the FCM sender, or a no-op one without credentials.
func newSender(cfg config.Config, hc *http.Client) (fcm.Sender, error) {
	if cfg.FCMCredentials == "" {
		return fcm.Nop{}, nil
	}

	raw, err := os.ReadFile(cfg.FCMCredentials)
	if err != nil {
		return nil, fmt.Errorf("read fcm credentials: %w", err)
	}

	return fcm.NewHTTP(raw, hc)
}

// addBots registers the bots from the bots file and returns their number.
func addBots(cfg config.Config, hub *game.Hub, d *dict.Dictionary, clk game.Clock) (int, error) {
	if !cfg.BotsEnabled {
		return 0, nil
	}

	path := cfg.BotsFile
	if path == "" {
		path = filepath.Join(cfg.DataDir, "bots.json")
	}

	specs, err := game.LoadBots(path)
	if err != nil {
		return 0, err
	}

	for _, s := range specs {
		hub.AddBot(game.NewBot(s, hub, d, clk, rand.New(rand.NewSource(time.Now().UnixNano()+s.UserID))))
	}

	return len(specs), nil
}

func tlsMode(cfg config.Config) string {
	if cfg.TLS() == config.TLSAutocert {
		return "autocert"
	}

	return "files"
}

// newHTTPServer sets no ReadTimeout or WriteTimeout: they would also cut
// the hijacked game sockets. Request bodies get a deadline in the handlers.
func newHTTPServer(h http.Handler) *http.Server {
	return &http.Server{
		Handler:           h,
		ReadHeaderTimeout: headerTimeout,
		IdleTimeout:       idleTimeout,
	}
}

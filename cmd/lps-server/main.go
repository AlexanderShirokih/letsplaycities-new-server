// Command lps-server runs the "Сыграем в Города" game server.
package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/config"
	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/game"
)

func main() {
	if err := serveMain(); err != nil {
		log.Fatalf("lps-server: %v", err)
	}
}

func serveMain() error {
	cfg, err := config.Load(os.Getenv)
	if err != nil {
		return err
	}

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGTERM, syscall.SIGINT)
	defer stop()

	ln, err := listen(ctx, cfg)
	if err != nil {
		return err
	}

	return run(ctx, cfg, ln, game.RealClock{})
}

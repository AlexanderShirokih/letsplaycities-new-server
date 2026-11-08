// Command lps-migrate converts the old MariaDB dump (backup.sql.gz) into the SQLite database.
//
//	LPS_NATIVE_PEPPER=… lps-migrate -in backup.sql.gz -out lps.db [-force]
//
// Exit codes: 0 report OK, 1 report not OK, 2 usage or migration error.
package main

import (
	"context"
	"flag"
	"fmt"
	"io"
	"os"
	"os/signal"
	"syscall"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/migrate"
)

const (
	exitOK    = 0
	exitNotOK = 1
	exitError = 2
)

// botIDs are the bot profiles; they get no identities.
var botIDs = []int64{12407, 12408, 14397, 14398, 14399, 14400}

func main() {
	os.Exit(run(os.Args[1:], os.Getenv, os.Stdout, os.Stderr))
}

func run(args []string, getenv func(string) string, stdout, stderr io.Writer) int {
	fs := flag.NewFlagSet("lps-migrate", flag.ContinueOnError)
	fs.SetOutput(stderr)

	in := fs.String("in", "", "MariaDB dump, gzip (backup.sql.gz)")
	out := fs.String("out", "", "SQLite database to create (lps.db)")
	force := fs.Bool("force", false, "replace an existing -out")

	if err := fs.Parse(args); err != nil {
		return exitError
	}

	if *in == "" || *out == "" || fs.NArg() > 0 {
		_, _ = fmt.Fprintln(stderr, "usage: lps-migrate -in backup.sql.gz -out lps.db [-force]")

		return exitError
	}

	pepper := getenv("LPS_NATIVE_PEPPER")
	if pepper == "" {
		_, _ = fmt.Fprintln(stderr, "lps-migrate: LPS_NATIVE_PEPPER is required")

		return exitError
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	rep, err := migrate.Run(ctx, migrate.Options{In: *in, Out: *out, Force: *force, Pepper: []byte(pepper), BotIDs: botIDs})
	if err != nil {
		_, _ = fmt.Fprintf(stderr, "lps-migrate: %v\n", err)

		return exitError
	}

	_, _ = fmt.Fprint(stdout, rep.String())

	if !rep.OK() {
		return exitNotOK
	}

	return exitOK
}

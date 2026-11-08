package store

import (
	"path/filepath"
	"testing"
)

func TestFilePragmas(t *testing.T) {
	s, err := Open(filepath.Join(t.TempDir(), "lps.db"))
	if err != nil {
		t.Fatal(err)
	}

	defer func() { _ = s.Close() }()

	var (
		mode string
		sync int
	)

	if err := s.db.QueryRow(`PRAGMA journal_mode`).Scan(&mode); err != nil {
		t.Fatal(err)
	}

	if err := s.db.QueryRow(`PRAGMA synchronous`).Scan(&sync); err != nil {
		t.Fatal(err)
	}

	// synchronous=NORMAL (1) is safe in WAL mode and saves an fsync per commit.
	if mode != "wal" || sync != 1 {
		t.Fatalf("journal_mode %s, synchronous %d; want wal, 1", mode, sync)
	}
}

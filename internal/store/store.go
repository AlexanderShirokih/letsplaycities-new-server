// Package store keeps all persistent state in SQLite.
package store

import (
	"context"
	"database/sql"
	_ "embed"
	"errors"
	"fmt"
	"net/url"
	"time"

	_ "modernc.org/sqlite"
)

//go:embed schema.sql
var schema string

// ErrNotFound is returned when the requested row does not exist.
var ErrNotFound = errors.New("not found")

// Store is a SQLite-backed storage. Methods are added by files in this package.
type Store struct {
	db *sql.DB
}

// Open opens (and, on first use, initializes) the database at path.
// Use ":memory:" for an in-memory database.
func Open(path string) (*Store, error) {
	q := "?_pragma=foreign_keys(1)&_pragma=busy_timeout(5000)"
	dsn := "file::memory:" + q
	if path != ":memory:" {
		dsn = "file:" + (&url.URL{Path: path}).EscapedPath() + q + "&_pragma=journal_mode(WAL)"
	}
	db, err := sql.Open("sqlite", dsn)
	if err != nil {
		return nil, err
	}
	db.SetMaxOpenConns(1)
	s := &Store{db: db}
	if err := s.migrate(); err != nil {
		db.Close()
		return nil, fmt.Errorf("init schema: %w", err)
	}
	return s, nil
}

func (s *Store) migrate() error {
	return s.inTx(context.Background(), func(tx *sql.Tx) error {
		var v int
		if err := tx.QueryRow(`PRAGMA user_version`).Scan(&v); err != nil {
			return err
		}
		if v != 0 {
			return nil
		}
		if _, err := tx.Exec(schema); err != nil {
			return err
		}
		_, err := tx.Exec(`PRAGMA user_version = 1`)
		return err
	})
}

// Close closes the database.
func (s *Store) Close() error { return s.db.Close() }

// inTx runs fn in a transaction, committing on nil and rolling back otherwise.
func (s *Store) inTx(ctx context.Context, fn func(*sql.Tx) error) error {
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	if err := fn(tx); err != nil {
		tx.Rollback()
		return err
	}
	return tx.Commit()
}

func ms(t time.Time) int64     { return t.UnixMilli() }
func fromMS(v int64) time.Time { return time.UnixMilli(v).UTC() }

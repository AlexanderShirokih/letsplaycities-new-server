package store

import (
	"context"
	"database/sql"
	"errors"
	"strings"
	"time"
)

// Role is a user's privilege level.
type Role int

const (
	RoleBanned  Role = 0
	RoleRegular Role = 1
	RoleAdmin   Role = 2
)

// User is an account row; Provider is "" for accounts without an identity (bots).
type User struct {
	ID                    int64
	Name                  string
	CreatedAt, LastSeenAt time.Time
	AvatarHash            string // "" = none
	FCMToken              string
	Role                  Role
	Provider              string
}

const userSelect = `SELECT u.id, u.name, u.created_at, u.last_seen_at,
	COALESCE(u.avatar_hash, ''), COALESCE(u.fcm_token, ''), u.role, COALESCE(i.provider, '')
	FROM users u LEFT JOIN identities i ON i.user_id = u.id `

type rowScanner interface{ Scan(...any) error }

func scanUser(r rowScanner) (User, error) {
	var u User
	var created, seen int64
	var role int
	err := r.Scan(&u.ID, &u.Name, &created, &seen, &u.AvatarHash, &u.FCMToken, &role, &u.Provider)
	if errors.Is(err, sql.ErrNoRows) {
		return User{}, ErrNotFound
	}
	if err != nil {
		return User{}, err
	}
	u.CreatedAt, u.LastSeenAt, u.Role = fromMS(created), fromMS(seen), Role(role)
	return u, nil
}

func (s *Store) UserByIdentity(ctx context.Context, provider, uid string) (User, error) {
	return scanUser(s.db.QueryRowContext(ctx,
		userSelect+`WHERE i.provider = ? AND i.uid = ?`, provider, uid))
}

// CreateUser inserts a user and its identity atomically.
func (s *Store) CreateUser(ctx context.Context, provider, uid, name string, now time.Time) (User, error) {
	var id int64
	err := s.inTx(ctx, func(tx *sql.Tx) error {
		res, err := tx.ExecContext(ctx,
			`INSERT INTO users(name, created_at, last_seen_at) VALUES(?, ?, ?)`, name, ms(now), ms(now))
		if err != nil {
			return err
		}
		if id, err = res.LastInsertId(); err != nil {
			return err
		}
		_, err = tx.ExecContext(ctx,
			`INSERT INTO identities(provider, uid, user_id) VALUES(?, ?, ?)`, provider, uid, id)
		return err
	})
	if err != nil {
		return User{}, err
	}
	return s.User(ctx, id)
}

func (s *Store) User(ctx context.Context, id int64) (User, error) {
	return scanUser(s.db.QueryRowContext(ctx, userSelect+`WHERE u.id = ?`, id))
}

func (s *Store) execOne(ctx context.Context, query string, args ...any) error {
	res, err := s.db.ExecContext(ctx, query, args...)
	if err != nil {
		return err
	}
	if n, err := res.RowsAffected(); err != nil {
		return err
	} else if n == 0 {
		return ErrNotFound
	}
	return nil
}

func (s *Store) UpdateName(ctx context.Context, id int64, name string) error {
	return s.execOne(ctx, `UPDATE users SET name = ? WHERE id = ?`, name, id)
}

// UpdateFCMToken stores token; an empty token is stored as NULL.
func (s *Store) UpdateFCMToken(ctx context.Context, id int64, token string) error {
	var v any
	if token != "" {
		v = token
	}
	return s.execOne(ctx, `UPDATE users SET fcm_token = ? WHERE id = ?`, v, id)
}

func (s *Store) TouchLastSeen(ctx context.Context, id int64, now time.Time) error {
	return s.execOne(ctx, `UPDATE users SET last_seen_at = ? WHERE id = ?`, ms(now), id)
}

func (s *Store) CreateSession(ctx context.Context, tokenHash []byte, userID int64, now time.Time) error {
	_, err := s.db.ExecContext(ctx,
		`INSERT INTO sessions(token_hash, user_id, created_at, last_used_at) VALUES(?, ?, ?, ?)`,
		tokenHash, userID, ms(now), ms(now))
	return err
}

func (s *Store) UserBySession(ctx context.Context, tokenHash []byte) (User, error) {
	return scanUser(s.db.QueryRowContext(ctx,
		userSelect+`JOIN sessions s ON s.user_id = u.id WHERE s.token_hash = ?`, tokenHash))
}

func (s *Store) DeleteSession(ctx context.Context, tokenHash []byte) error {
	_, err := s.db.ExecContext(ctx, `DELETE FROM sessions WHERE token_hash = ?`, tokenHash)
	return err
}

// IsForbiddenName reports whether any whitespace-separated word of name
// is listed in forbidden_logins, ignoring case.
func (s *Store) IsForbiddenName(ctx context.Context, name string) (bool, error) {
	rows, err := s.db.QueryContext(ctx, `SELECT value FROM forbidden_logins`)
	if err != nil {
		return false, err
	}
	defer rows.Close()
	words := map[string]bool{}
	for _, w := range strings.Fields(name) {
		words[strings.ToLower(w)] = true
	}
	for rows.Next() {
		var v string
		if err := rows.Scan(&v); err != nil {
			return false, err
		}
		if words[strings.ToLower(v)] {
			return true, nil
		}
	}
	return false, rows.Err()
}

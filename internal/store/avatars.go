package store

import (
	"context"
	"database/sql"
	"errors"
)

type Avatar struct {
	MIME string
	Data []byte
}

// SetAvatar stores the avatar and users.avatar_hash in one transaction.
func (s *Store) SetAvatar(ctx context.Context, userID int64, a Avatar, hash string) error {
	return s.inTx(ctx, func(tx *sql.Tx) error {
		res, err := tx.ExecContext(ctx, `UPDATE users SET avatar_hash = ? WHERE id = ?`, hash, userID)
		if err != nil {
			return err
		}
		if n, err := res.RowsAffected(); err != nil {
			return err
		} else if n == 0 {
			return ErrNotFound
		}
		_, err = tx.ExecContext(ctx,
			`INSERT INTO avatars(user_id, mime, data) VALUES(?, ?, ?)
			 ON CONFLICT(user_id) DO UPDATE SET mime = excluded.mime, data = excluded.data`,
			userID, a.MIME, a.Data)
		return err
	})
}

// Avatar returns the avatar and its hash, or ErrNotFound.
func (s *Store) Avatar(ctx context.Context, userID int64) (Avatar, string, error) {
	var a Avatar
	var hash string
	err := s.db.QueryRowContext(ctx,
		`SELECT a.mime, a.data, COALESCE(u.avatar_hash, '') FROM avatars a JOIN users u ON u.id = a.user_id
		 WHERE a.user_id = ?`, userID).Scan(&a.MIME, &a.Data, &hash)
	if errors.Is(err, sql.ErrNoRows) {
		return Avatar{}, "", ErrNotFound
	}
	return a, hash, err
}

func (s *Store) DeleteAvatar(ctx context.Context, userID int64) error {
	return s.inTx(ctx, func(tx *sql.Tx) error {
		if _, err := tx.ExecContext(ctx, `DELETE FROM avatars WHERE user_id = ?`, userID); err != nil {
			return err
		}
		_, err := tx.ExecContext(ctx, `UPDATE users SET avatar_hash = NULL WHERE id = ?`, userID)
		return err
	})
}

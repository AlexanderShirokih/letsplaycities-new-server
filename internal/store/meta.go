package store

import (
	"context"
	"database/sql"
	"errors"
)

// ErrPepperMismatch means the database was created with another pepper:
// every Native login would miss its account.
var ErrPepperMismatch = errors.New("LPS_NATIVE_PEPPER differs from the pepper this database was created with")

// CheckPepper compares check with the pepper check stored in meta. A fresh
// database has none yet and stores check.
func (s *Store) CheckPepper(ctx context.Context, check string) error {
	return s.inTx(ctx, func(tx *sql.Tx) error {
		var stored string

		err := tx.QueryRowContext(ctx, `SELECT pepper_check FROM meta WHERE id = 1`).Scan(&stored)
		if errors.Is(err, sql.ErrNoRows) {
			_, err = tx.ExecContext(ctx, `INSERT INTO meta (id, pepper_check) VALUES (1, ?)`, check)

			return err
		}

		if err != nil {
			return err
		}

		if stored != check {
			return ErrPepperMismatch
		}

		return nil
	})
}

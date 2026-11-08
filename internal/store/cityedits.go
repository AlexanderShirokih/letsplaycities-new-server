package store

import "context"

// CityEdit is a user's request to add, change or remove a city.
type CityEdit struct {
	ID                                int64
	OldCountry, NewCountry            int
	OldName, NewName, Reason, Verdict *string
	Status                            string
}

// CityEdits returns the user's edits, newest first.
func (s *Store) CityEdits(ctx context.Context, userID int64, limit int) ([]CityEdit, error) {
	rows, err := s.db.QueryContext(ctx,
		`SELECT id, COALESCE(old_country, 0), COALESCE(new_country, 0), old_name, new_name, reason, verdict, status
		 FROM city_edits WHERE user_id = ? ORDER BY id DESC LIMIT ?`, userID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []CityEdit
	for rows.Next() {
		var e CityEdit
		if err := rows.Scan(&e.ID, &e.OldCountry, &e.NewCountry, &e.OldName, &e.NewName, &e.Reason, &e.Verdict, &e.Status); err != nil {
			return nil, err
		}
		out = append(out, e)
	}
	return out, rows.Err()
}

func (s *Store) CountNewCityEdits(ctx context.Context, userID int64) (int, error) {
	var n int
	err := s.db.QueryRowContext(ctx,
		`SELECT COUNT(*) FROM city_edits WHERE user_id = ? AND status = 'NEW'`, userID).Scan(&n)
	return n, err
}

// AddCityEdit stores e with status NEW and returns its id.
func (s *Store) AddCityEdit(ctx context.Context, userID int64, e CityEdit) (int64, error) {
	res, err := s.db.ExecContext(ctx,
		`INSERT INTO city_edits(user_id, old_country, new_country, old_name, new_name, reason, verdict, status)
		 VALUES(?, ?, ?, ?, ?, ?, ?, 'NEW')`,
		userID, e.OldCountry, e.NewCountry, e.OldName, e.NewName, e.Reason, e.Verdict)
	if err != nil {
		return 0, err
	}
	return res.LastInsertId()
}

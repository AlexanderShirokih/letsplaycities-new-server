package store

import (
	"context"
	"time"
)

type Game struct {
	ID, StarterID, InvitedID int64
	StartedAt                time.Time
	Duration                 time.Duration
	Words                    int
}

func (s *Store) RecordGame(ctx context.Context, g Game) (int64, error) {
	res, err := s.db.ExecContext(ctx,
		`INSERT INTO games(starter_id, invited_id, started_at, duration_sec, words) VALUES(?, ?, ?, ?, ?)`,
		g.StarterID, g.InvitedID, ms(g.StartedAt), int64(g.Duration/time.Second), g.Words)
	if err != nil {
		return 0, err
	}
	return res.LastInsertId()
}

type HistoryEntry struct {
	Game     Game
	Opponent User
	IsFriend bool
}

// History returns userID's games, newest first. with filters by opponent (0 = all);
// before is a cursor (0 = from the start) meaning id < before. next is the id of the
// last returned game when more remain, otherwise 0.
func (s *Store) History(ctx context.Context, userID, with, before int64, limit int) ([]HistoryEntry, int64, error) {
	if limit <= 0 {
		return nil, 0, nil
	}
	rows, err := s.db.QueryContext(ctx,
		`SELECT id, starter_id, invited_id, started_at, duration_sec, words FROM games
		 WHERE (starter_id = ?1 OR invited_id = ?1)
		   AND (?2 = 0 OR starter_id = ?2 OR invited_id = ?2)
		   AND (?3 = 0 OR id < ?3)
		 ORDER BY started_at DESC, id DESC LIMIT ?4`, userID, with, before, limit+1)
	if err != nil {
		return nil, 0, err
	}
	var games []Game
	for rows.Next() {
		var g Game
		var started, dur int64
		if err := rows.Scan(&g.ID, &g.StarterID, &g.InvitedID, &started, &dur, &g.Words); err != nil {
			rows.Close()
			return nil, 0, err
		}
		g.StartedAt, g.Duration = fromMS(started), time.Duration(dur)*time.Second
		games = append(games, g)
	}
	if err := rows.Close(); err != nil {
		return nil, 0, err
	}
	if err := rows.Err(); err != nil {
		return nil, 0, err
	}
	var next int64
	if len(games) > limit {
		games = games[:limit]
		next = games[limit-1].ID
	}
	out := make([]HistoryEntry, len(games))
	for i, g := range games {
		opp := g.InvitedID
		if opp == userID {
			opp = g.StarterID
		}
		u, err := s.User(ctx, opp)
		if err != nil {
			return nil, 0, err
		}
		r, err := s.Relation(ctx, userID, opp)
		if err != nil {
			return nil, 0, err
		}
		out[i] = HistoryEntry{Game: g, Opponent: u, IsFriend: r.Friendship == FriendFriends}
	}
	return out, next, nil
}

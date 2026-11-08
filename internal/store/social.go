package store

import (
	"context"
	"database/sql"
	"time"
)

// Friendship is the friendship state from the viewer's perspective.
type Friendship string

const (
	FriendNone     Friendship = "none"
	FriendFriends  Friendship = "friends"
	FriendIncoming Friendship = "incoming"
	FriendOutgoing Friendship = "outgoing"
)

// Blocked is the block state from the viewer's perspective.
type Blocked string

const (
	BlockedNone Blocked = "none"
	BlockedByMe Blocked = "byMe"
	BlockedMe   Blocked = "me"
	BlockedBoth Blocked = "both"
)

type Relation struct {
	Friendship Friendship
	Blocked    Blocked
}

// FriendEntry is a friend or a pending request; Status is from the caller's perspective.
type FriendEntry struct {
	User   User
	Status Friendship
}

func (s *Store) Relation(ctx context.Context, viewer, other int64) (Relation, error) {
	r := Relation{Friendship: FriendNone, Blocked: BlockedNone}

	var err error

	if r.Friendship, err = s.friendshipBetween(ctx, viewer, other); err != nil {
		return r, err
	}

	if r.Blocked, err = s.blockedBetween(ctx, viewer, other); err != nil {
		return r, err
	}

	return r, nil
}

func (s *Store) friendshipBetween(ctx context.Context, viewer, other int64) (Friendship, error) {
	f := FriendNone

	rows, err := s.db.QueryContext(ctx,
		`SELECT sender_id, accepted FROM friendships
		 WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)`,
		viewer, other, other, viewer)
	if err != nil {
		return f, err
	}

	for rows.Next() {
		var (
			sender   int64
			accepted bool
		)
		if err := rows.Scan(&sender, &accepted); err != nil {
			_ = rows.Close()
			return f, err
		}

		switch {
		case accepted:
			f = FriendFriends
		case f == FriendFriends:
		case sender == viewer:
			f = FriendOutgoing
		default:
			f = FriendIncoming
		}
	}

	if err := rows.Close(); err != nil {
		return f, err
	}

	if err := rows.Err(); err != nil {
		return f, err
	}

	return f, nil
}

func (s *Store) blockedBetween(ctx context.Context, viewer, other int64) (Blocked, error) {
	var byMe, me bool

	err := s.db.QueryRowContext(ctx,
		`SELECT COALESCE(MAX(blocker_id = ?), 0), COALESCE(MAX(blocker_id = ?), 0)
		 FROM blocks WHERE (blocker_id = ? AND blocked_id = ?) OR (blocker_id = ? AND blocked_id = ?)`,
		viewer, other, viewer, other, other, viewer).Scan(&byMe, &me)
	if err != nil {
		return BlockedNone, err
	}

	switch {
	case byMe && me:
		return BlockedBoth, nil
	case byMe:
		return BlockedByMe, nil
	case me:
		return BlockedMe, nil
	}

	return BlockedNone, nil
}

// Friends returns accepted friends and pending requests in either direction, ordered by name.
func (s *Store) Friends(ctx context.Context, userID int64) ([]FriendEntry, error) {
	rows, err := s.db.QueryContext(ctx,
		`SELECT u.id, u.name, u.created_at, u.last_seen_at,
			COALESCE(u.avatar_hash, ''), COALESCE(u.fcm_token, ''), u.role, COALESCE(i.provider, ''),
			f.sender_id, f.accepted
		 FROM friendships f
		 JOIN users u ON u.id = CASE WHEN f.sender_id = ?1 THEN f.receiver_id ELSE f.sender_id END
		 LEFT JOIN identities i ON i.user_id = u.id
		 WHERE f.sender_id = ?1 OR f.receiver_id = ?1
		 ORDER BY u.name, u.id`, userID)
	if err != nil {
		return nil, err
	}
	defer func() { _ = rows.Close() }()

	var out []FriendEntry

	for rows.Next() {
		var (
			sender   int64
			accepted bool
		)

		u, err := scanUser(extraScanner{rows, []any{&sender, &accepted}})
		if err != nil {
			return nil, err
		}

		st := FriendIncoming

		switch {
		case accepted:
			st = FriendFriends
		case sender == userID:
			st = FriendOutgoing
		}

		out = append(out, FriendEntry{User: u, Status: st})
	}

	return out, rows.Err()
}

// extraScanner scans extra destinations after the user columns.
type extraScanner struct {
	rowScanner
	extra []any
}

func (e extraScanner) Scan(dest ...any) error { return e.rowScanner.Scan(append(dest, e.extra...)...) }

// RequestOrAcceptFriend sends a request from -> to, or accepts a pending request to -> from.
// Repeating an existing request in the same direction is a no-op.
func (s *Store) RequestOrAcceptFriend(ctx context.Context, from, to int64, now time.Time) error {
	return s.inTx(ctx, func(tx *sql.Tx) error {
		res, err := tx.ExecContext(ctx,
			`UPDATE friendships SET accepted = 1 WHERE sender_id = ? AND receiver_id = ? AND accepted = 0`, to, from)
		if err != nil {
			return err
		}

		if n, err := res.RowsAffected(); err != nil || n > 0 {
			return err
		}

		var exists bool
		if err := tx.QueryRowContext(ctx,
			`SELECT EXISTS(SELECT 1 FROM friendships
			 WHERE (sender_id = ?1 AND receiver_id = ?2) OR (sender_id = ?2 AND receiver_id = ?1))`,
			from, to).Scan(&exists); err != nil || exists {
			return err
		}

		_, err = tx.ExecContext(ctx,
			`INSERT INTO friendships(sender_id, receiver_id, accepted, created_at) VALUES(?, ?, 0, ?)`,
			from, to, ms(now))

		return err
	})
}

// RemoveFriendship deletes the friendship or request between a and b in any direction.
func (s *Store) RemoveFriendship(ctx context.Context, a, b int64) error {
	_, err := s.db.ExecContext(ctx,
		`DELETE FROM friendships WHERE (sender_id = ?1 AND receiver_id = ?2) OR (sender_id = ?2 AND receiver_id = ?1)`, a, b)

	return err
}

// Block records the block and removes any friendship between the users.
func (s *Store) Block(ctx context.Context, blocker, blocked int64) error {
	return s.inTx(ctx, func(tx *sql.Tx) error {
		if _, err := tx.ExecContext(ctx,
			`INSERT OR IGNORE INTO blocks(blocker_id, blocked_id) VALUES(?, ?)`, blocker, blocked); err != nil {
			return err
		}

		_, err := tx.ExecContext(ctx,
			`DELETE FROM friendships WHERE (sender_id = ?1 AND receiver_id = ?2) OR (sender_id = ?2 AND receiver_id = ?1)`,
			blocker, blocked)

		return err
	})
}

func (s *Store) Unblock(ctx context.Context, blocker, blocked int64) error {
	_, err := s.db.ExecContext(ctx,
		`DELETE FROM blocks WHERE blocker_id = ? AND blocked_id = ?`, blocker, blocked)

	return err
}

// Blocks returns the users blocked by blocker, ordered by name.
func (s *Store) Blocks(ctx context.Context, blocker int64) ([]User, error) {
	rows, err := s.db.QueryContext(ctx,
		userSelect+`JOIN blocks b ON b.blocked_id = u.id WHERE b.blocker_id = ? ORDER BY u.name, u.id`, blocker)
	if err != nil {
		return nil, err
	}
	defer func() { _ = rows.Close() }()

	var out []User

	for rows.Next() {
		u, err := scanUser(rows)
		if err != nil {
			return nil, err
		}

		out = append(out, u)
	}

	return out, rows.Err()
}

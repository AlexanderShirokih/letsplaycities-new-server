package store

import (
	"context"
	"errors"
	"path/filepath"
	"testing"
	"time"
)

func openMem(t *testing.T) *Store {
	t.Helper()
	s, err := Open(":memory:")
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { s.Close() })
	return s
}

func TestCreateAndFindByIdentity(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	now := time.UnixMilli(1700000000123)
	created, err := s.CreateUser(ctx, "native", "h1", "Вася", now)
	if err != nil {
		t.Fatal(err)
	}
	got, err := s.UserByIdentity(ctx, "native", "h1")
	if err != nil {
		t.Fatal(err)
	}
	if got.ID != created.ID || got.Role != RoleRegular || got.Provider != "native" || got.Name != "Вася" {
		t.Fatalf("unexpected user: %+v", got)
	}
	if !got.CreatedAt.Equal(now) || got.CreatedAt.Location() != time.UTC || !got.LastSeenAt.Equal(now) {
		t.Fatalf("unexpected times: %+v", got)
	}
}

func TestUnknownIdentity(t *testing.T) {
	s := openMem(t)
	if _, err := s.UserByIdentity(context.Background(), "native", "nope"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("got %v", err)
	}
	if _, err := s.User(context.Background(), 42); !errors.Is(err, ErrNotFound) {
		t.Fatalf("got %v", err)
	}
}

func TestDuplicateIdentityRollsBackUser(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	if _, err := s.CreateUser(ctx, "vk", "1", "A", time.Now()); err != nil {
		t.Fatal(err)
	}
	if _, err := s.CreateUser(ctx, "vk", "1", "B", time.Now()); err == nil {
		t.Fatal("expected error")
	}
	var n int
	if err := s.db.QueryRow(`SELECT COUNT(*) FROM users`).Scan(&n); err != nil || n != 1 {
		t.Fatalf("users=%d err=%v", n, err)
	}
}

func TestUpdates(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	u, _ := s.CreateUser(ctx, "google", "g", "Old", time.UnixMilli(1000))
	if err := s.UpdateName(ctx, u.ID, "New"); err != nil {
		t.Fatal(err)
	}
	if err := s.UpdateFCMToken(ctx, u.ID, "tok"); err != nil {
		t.Fatal(err)
	}
	if err := s.TouchLastSeen(ctx, u.ID, time.UnixMilli(5000)); err != nil {
		t.Fatal(err)
	}
	got, _ := s.User(ctx, u.ID)
	if got.Name != "New" || got.FCMToken != "tok" || !got.LastSeenAt.Equal(time.UnixMilli(5000)) {
		t.Fatalf("%+v", got)
	}
	if err := s.UpdateFCMToken(ctx, u.ID, ""); err != nil {
		t.Fatal(err)
	}
	var isNull bool
	s.db.QueryRow(`SELECT fcm_token IS NULL FROM users WHERE id=?`, u.ID).Scan(&isNull)
	if !isNull {
		t.Fatal("empty token must be stored as NULL")
	}
	for _, err := range []error{
		s.UpdateName(ctx, 999, "x"), s.UpdateFCMToken(ctx, 999, "x"), s.TouchLastSeen(ctx, 999, time.Now()),
	} {
		if !errors.Is(err, ErrNotFound) {
			t.Fatalf("got %v", err)
		}
	}
}

func TestUserWithoutIdentityHasEmptyProvider(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	res, err := s.db.Exec(`INSERT INTO users(name, created_at, last_seen_at) VALUES('bot', 1, 1)`)
	if err != nil {
		t.Fatal(err)
	}
	id, _ := res.LastInsertId()
	u, err := s.User(ctx, id)
	if err != nil || u.Provider != "" || u.Role != RoleRegular {
		t.Fatalf("%+v %v", u, err)
	}
}

func TestSessionRoundTrip(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	u, _ := s.CreateUser(ctx, "native", "h1", "Вася", time.Now())
	hash := []byte("0123456789abcdef0123456789abcdef")
	if err := s.CreateSession(ctx, hash, u.ID, time.Now()); err != nil {
		t.Fatal(err)
	}
	got, err := s.UserBySession(ctx, hash)
	if err != nil || got.ID != u.ID || got.Provider != "native" {
		t.Fatalf("%+v %v", got, err)
	}
	if err := s.DeleteSession(ctx, hash); err != nil {
		t.Fatal(err)
	}
	if _, err := s.UserBySession(ctx, hash); !errors.Is(err, ErrNotFound) {
		t.Fatalf("got %v", err)
	}
}

func TestForbiddenName(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	if _, err := s.db.Exec(`INSERT INTO forbidden_logins(value) VALUES('admin')`); err != nil {
		t.Fatal(err)
	}
	for name, want := range map[string]bool{"Super ADMIN": true, "Вася": false, "": false, "administrator": false} {
		got, err := s.IsForbiddenName(ctx, name)
		if err != nil || got != want {
			t.Errorf("%q: got %v err %v", name, got, err)
		}
	}
}

func TestCascadeDelete(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	u, _ := s.CreateUser(ctx, "native", "h", "A", time.Now())
	s.CreateSession(ctx, []byte("t"), u.ID, time.Now())
	if _, err := s.db.Exec(`DELETE FROM users WHERE id=?`, u.ID); err != nil {
		t.Fatal(err)
	}
	for _, tbl := range []string{"identities", "sessions"} {
		var n int
		s.db.QueryRow(`SELECT COUNT(*) FROM ` + tbl).Scan(&n)
		if n != 0 {
			t.Errorf("%s not cascaded", tbl)
		}
	}
}

func TestReopenKeepsSchema(t *testing.T) {
	path := filepath.Join(t.TempDir(), "lps.db")
	ctx := context.Background()
	s, err := Open(path)
	if err != nil {
		t.Fatal(err)
	}
	u, err := s.CreateUser(ctx, "native", "h1", "Вася", time.Now())
	if err != nil {
		t.Fatal(err)
	}
	if err := s.Close(); err != nil {
		t.Fatal(err)
	}
	s, err = Open(path)
	if err != nil {
		t.Fatal(err)
	}
	defer s.Close()
	got, err := s.UserByIdentity(ctx, "native", "h1")
	if err != nil || got.ID != u.ID {
		t.Fatalf("%+v %v", got, err)
	}
}

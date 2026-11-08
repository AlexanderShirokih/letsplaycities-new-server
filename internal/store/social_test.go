package store

import (
	"context"
	"testing"
	"time"
)

func mkUsers(t *testing.T, s *Store, names ...string) []int64 {
	t.Helper()
	ids := make([]int64, len(names))
	for i, n := range names {
		u, err := s.CreateUser(context.Background(), "native", "uid-"+n, n, time.UnixMilli(1700000000000))
		if err != nil {
			t.Fatal(err)
		}
		ids[i] = u.ID
	}
	return ids
}

func rel(t *testing.T, s *Store, v, o int64) Relation {
	t.Helper()
	r, err := s.Relation(context.Background(), v, o)
	if err != nil {
		t.Fatal(err)
	}
	return r
}

func TestFriendFlow(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	u := mkUsers(t, s, "A", "B")
	a, b := u[0], u[1]
	now := time.UnixMilli(1700000000000)

	if err := s.RequestOrAcceptFriend(ctx, a, b, now); err != nil {
		t.Fatal(err)
	}
	if err := s.RequestOrAcceptFriend(ctx, a, b, now); err != nil { // no-op
		t.Fatal(err)
	}
	if r := rel(t, s, a, b); r.Friendship != "outgoing" || r.Blocked != "none" {
		t.Fatalf("a->b: %+v", r)
	}
	if r := rel(t, s, b, a); r.Friendship != "incoming" {
		t.Fatalf("b->a: %+v", r)
	}
	fs, err := s.Friends(ctx, b)
	if err != nil || len(fs) != 1 || fs[0].User.ID != a || fs[0].Status != "incoming" {
		t.Fatalf("friends of b: %+v %v", fs, err)
	}
	if err := s.RequestOrAcceptFriend(ctx, b, a, now); err != nil {
		t.Fatal(err)
	}
	if rel(t, s, a, b).Friendship != "friends" || rel(t, s, b, a).Friendship != "friends" {
		t.Fatal("expected friends")
	}
	fs, _ = s.Friends(ctx, a)
	if len(fs) != 1 || fs[0].Status != "friends" {
		t.Fatalf("friends of a: %+v", fs)
	}
	if err := s.RemoveFriendship(ctx, b, a); err != nil {
		t.Fatal(err)
	}
	if rel(t, s, a, b).Friendship != "none" || rel(t, s, b, a).Friendship != "none" {
		t.Fatal("expected none")
	}
}

func TestBlockRemovesFriendship(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	u := mkUsers(t, s, "A", "B")
	a, b := u[0], u[1]
	now := time.UnixMilli(1700000000000)
	s.RequestOrAcceptFriend(ctx, a, b, now)
	s.RequestOrAcceptFriend(ctx, b, a, now)
	if err := s.Block(ctx, a, b); err != nil {
		t.Fatal(err)
	}
	if r := rel(t, s, a, b); r.Friendship != "none" || r.Blocked != "byMe" {
		t.Fatalf("a->b: %+v", r)
	}
	if r := rel(t, s, b, a); r.Blocked != "me" {
		t.Fatalf("b->a: %+v", r)
	}
	bl, err := s.Blocks(ctx, a)
	if err != nil || len(bl) != 1 || bl[0].ID != b {
		t.Fatalf("blocks: %+v %v", bl, err)
	}
	if err := s.Block(ctx, a, b); err != nil { // idempotent
		t.Fatal(err)
	}
	s.Block(ctx, b, a)
	if rel(t, s, a, b).Blocked != "both" {
		t.Fatal("expected both")
	}
	s.Unblock(ctx, a, b)
	if rel(t, s, a, b).Blocked != "me" {
		t.Fatal("expected me")
	}
}

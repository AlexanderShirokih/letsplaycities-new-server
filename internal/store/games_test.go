package store

import (
	"context"
	"testing"
	"time"
)

func TestHistoryPaging(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	u := mkUsers(t, s, "A", "B", "C")
	a, b, c := u[0], u[1], u[2]
	base := time.UnixMilli(1700000000000)
	for i := 0; i < 120; i++ {
		opp := b
		if i%3 == 2 {
			opp = c
		}
		g := Game{StarterID: a, InvitedID: opp, StartedAt: base.Add(time.Duration(i) * time.Minute), Duration: 90 * time.Second, Words: i}
		if i%2 == 1 {
			g.StarterID, g.InvitedID = opp, a
		}
		if _, err := s.RecordGame(ctx, g); err != nil {
			t.Fatal(err)
		}
	}
	s.RequestOrAcceptFriend(ctx, a, c, base)
	s.RequestOrAcceptFriend(ctx, c, a, base)

	p1, next, err := s.History(ctx, a, 0, 0, 50)
	if err != nil || len(p1) != 50 || next == 0 {
		t.Fatalf("p1 %d next %d %v", len(p1), next, err)
	}
	if p1[0].Game.Words != 119 || p1[0].Game.Duration != 90*time.Second || !p1[0].Game.StartedAt.Equal(base.Add(119*time.Minute)) {
		t.Fatalf("first: %+v", p1[0].Game)
	}
	for i := 1; i < len(p1); i++ {
		if p1[i].Game.StartedAt.After(p1[i-1].Game.StartedAt) {
			t.Fatal("not newest first")
		}
	}
	p2, next, err := s.History(ctx, a, 0, next, 50)
	if err != nil || len(p2) != 50 || next == 0 {
		t.Fatalf("p2 %d next %d %v", len(p2), next, err)
	}
	p3, next, err := s.History(ctx, a, 0, next, 50)
	if err != nil || len(p3) != 20 || next != 0 {
		t.Fatalf("p3 %d next %d %v", len(p3), next, err)
	}
	for _, e := range p1 {
		if (e.Opponent.ID == c) != e.IsFriend || (e.Opponent.ID != b && e.Opponent.ID != c) {
			t.Fatalf("bad entry %+v", e)
		}
	}
	pc, next, err := s.History(ctx, a, c, 0, 50)
	if err != nil || len(pc) != 40 || next != 0 {
		t.Fatalf("with c: %d next %d %v", len(pc), next, err)
	}
	for _, e := range pc {
		if e.Opponent.ID != c {
			t.Fatal("foreign opponent")
		}
	}
}

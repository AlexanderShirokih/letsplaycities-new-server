package store

import (
	"bytes"
	"context"
	"errors"
	"testing"
)

func TestAvatarRoundTrip(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	id := mkUsers(t, s, "A")[0]
	if _, _, err := s.Avatar(ctx, id); !errors.Is(err, ErrNotFound) {
		t.Fatalf("got %v", err)
	}
	if err := s.SetAvatar(ctx, id, Avatar{MIME: "image/png", Data: []byte{1, 2, 3}}, "h1"); err != nil {
		t.Fatal(err)
	}
	if err := s.SetAvatar(ctx, id, Avatar{MIME: "image/jpeg", Data: []byte{4, 5}}, "h2"); err != nil {
		t.Fatal(err)
	}
	u, _ := s.User(ctx, id)
	if u.AvatarHash != "h2" {
		t.Fatalf("hash %q", u.AvatarHash)
	}
	a, h, err := s.Avatar(ctx, id)
	if err != nil || h != "h2" || a.MIME != "image/jpeg" || !bytes.Equal(a.Data, []byte{4, 5}) {
		t.Fatalf("%+v %q %v", a, h, err)
	}
	if err := s.DeleteAvatar(ctx, id); err != nil {
		t.Fatal(err)
	}
	u, _ = s.User(ctx, id)
	if u.AvatarHash != "" {
		t.Fatalf("hash %q", u.AvatarHash)
	}
	if _, _, err := s.Avatar(ctx, id); !errors.Is(err, ErrNotFound) {
		t.Fatalf("got %v", err)
	}
	if err := s.SetAvatar(ctx, 999, Avatar{MIME: "image/png", Data: []byte{1}}, "x"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("unknown user: %v", err)
	}
}

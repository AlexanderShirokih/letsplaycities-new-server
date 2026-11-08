package store

import (
	"context"
	"errors"
	"strings"
	"testing"
)

func TestCheckPepper(t *testing.T) {
	ctx := context.Background()
	s := openMem(t)

	if err := s.CheckPepper(ctx, "check-a"); err != nil {
		t.Fatalf("fresh database: %v", err)
	}

	if err := s.CheckPepper(ctx, "check-a"); err != nil {
		t.Fatalf("same pepper: %v", err)
	}

	err := s.CheckPepper(ctx, "check-b")
	if !errors.Is(err, ErrPepperMismatch) {
		t.Fatalf("other pepper: %v, want ErrPepperMismatch", err)
	}

	for _, v := range []string{"check-a", "check-b"} {
		if strings.Contains(err.Error(), v) {
			t.Fatalf("error %q reveals a check value", err)
		}
	}
}

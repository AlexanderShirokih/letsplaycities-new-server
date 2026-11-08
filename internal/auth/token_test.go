package auth

import (
	"bytes"
	"testing"
)

func TestNativeUIDStable(t *testing.T) {
	a := NativeUID([]byte("pepper"), "dev1")
	if len(a) != 64 {
		t.Fatalf("len = %d", len(a))
	}

	if a != NativeUID([]byte("pepper"), "dev1") {
		t.Fatal("not stable")
	}

	if a == NativeUID([]byte("other"), "dev1") {
		t.Fatal("pepper ignored")
	}

	if a == NativeUID([]byte("pepper"), "dev2") {
		t.Fatal("device ignored")
	}
}

func TestTokenHash(t *testing.T) {
	tok, h := NewToken()
	if len(tok) != 43 || len(h) != 32 {
		t.Fatalf("tok %d hash %d", len(tok), len(h))
	}

	if !bytes.Equal(h, HashToken(tok)) {
		t.Fatal("hash mismatch")
	}

	tok2, _ := NewToken()
	if tok == tok2 {
		t.Fatal("tokens repeat")
	}
}

func TestPepperCheck(t *testing.T) {
	a := PepperCheck([]byte("pepper"))

	if a != NativeUID([]byte("pepper"), "lps-pepper-check") {
		t.Fatalf("check = %s, want hex HMAC of lps-pepper-check", a)
	}

	if a == PepperCheck([]byte("pepper\n")) {
		t.Fatal("pepper ignored")
	}
}

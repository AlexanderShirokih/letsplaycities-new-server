// Package auth provides session tokens, Native UIDs, rate limiting and
// social-provider token verification.
package auth

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
)

// NewToken returns a random session token and its SHA-256 hash for storage.
func NewToken() (token string, hash []byte) {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		panic("auth: crypto/rand failed: " + err.Error())
	}

	token = base64.RawURLEncoding.EncodeToString(b)

	return token, HashToken(token)
}

// HashToken returns sha256(token).
func HashToken(token string) []byte {
	h := sha256.Sum256([]byte(token))
	return h[:]
}

// NativeUID returns hex(HMAC-SHA256(pepper, deviceID)).
func NativeUID(pepper []byte, deviceID string) string {
	m := hmac.New(sha256.New, pepper)
	m.Write([]byte(deviceID))

	return hex.EncodeToString(m.Sum(nil))
}

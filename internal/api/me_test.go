package api

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"testing"
)

var png1x1 = []byte("\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89")

func TestPatchMe(t *testing.T) {
	e := newTestServer(t)
	a := e.native("dev1", "Вася")

	resp, data := e.do("PATCH", "/v1/me", a.Token, map[string]string{"name": "  Коля  ", "fcmToken": "fcm-1"})
	if resp.StatusCode != 200 {
		t.Fatalf("%d %s", resp.StatusCode, data)
	}

	if got := e.me(a.Token); got.Name != "Коля" {
		t.Fatalf("name %q", got.Name)
	}

	u, err := e.store.User(context.Background(), a.User.ID)
	if err != nil || u.FCMToken != "fcm-1" {
		t.Fatalf("fcm %q %v", u.FCMToken, err)
	}

	resp, data = e.do("PATCH", "/v1/me", a.Token, map[string]string{"name": "a"})
	e.wantError(resp, data, 400, "bad_name")

	if got := e.me(a.Token); got.Name != "Коля" {
		t.Fatalf("name changed on error: %q", got.Name)
	}
}

func TestAvatarUploadAndFetch(t *testing.T) {
	e := newTestServer(t)
	a := e.native("dev1", "Вася")

	resp, data := e.do("PUT", "/v1/me/avatar", a.Token, png1x1, "Content-Type", "image/png")
	if resp.StatusCode != 200 {
		t.Fatalf("%d %s", resp.StatusCode, data)
	}

	var u testUser

	_ = json.Unmarshal(data, &u)

	if u.AvatarHash == nil || len(*u.AvatarHash) != 16 {
		t.Fatalf("hash %v", u.AvatarHash)
	}

	hash := *u.AvatarHash
	path := fmt.Sprintf("/v1/avatars/%d", a.User.ID)

	resp, data = e.do("GET", path+"?v="+hash, "", nil)
	if resp.StatusCode != 200 || !bytes.Equal(data, png1x1) || resp.Header.Get("Content-Type") != "image/png" {
		t.Fatalf("fetch: %d %q", resp.StatusCode, resp.Header.Get("Content-Type"))
	}

	if resp.Header.Get("ETag") != `"`+hash+`"` || resp.Header.Get("Cache-Control") != "public, max-age=31536000, immutable" {
		t.Fatalf("headers %v", resp.Header)
	}

	resp, _ = e.do("GET", path+"?v=old", "", nil)
	if resp.Header.Get("Cache-Control") != "no-cache" || resp.Header.Get("ETag") != `"`+hash+`"` {
		t.Fatalf("headers %v", resp.Header)
	}

	resp, data = e.do("GET", path+"?v="+hash, "", nil, "If-None-Match", `"`+hash+`"`)
	if resp.StatusCode != 304 || len(data) != 0 {
		t.Fatalf("status %d", resp.StatusCode)
	}

	if got := e.me(a.Token); got.AvatarHash == nil || *got.AvatarHash != hash {
		t.Fatalf("me hash %v", got.AvatarHash)
	}

	resp, _ = e.do("DELETE", "/v1/me/avatar", a.Token, nil)
	if resp.StatusCode != 204 {
		t.Fatalf("delete %d", resp.StatusCode)
	}

	resp, data = e.do("GET", path, "", nil)
	e.wantError(resp, data, 404, "not_found")

	if got := e.me(a.Token); got.AvatarHash != nil {
		t.Fatalf("hash after delete %v", got.AvatarHash)
	}
}

func TestAvatarRejects(t *testing.T) {
	e := newTestServer(t)
	a := e.native("dev1", "Вася")
	e.do("PUT", "/v1/me/avatar", a.Token, png1x1, "Content-Type", "image/png")
	before := e.me(a.Token).AvatarHash

	jpeg := append([]byte("\xff\xd8\xff\xe0"), make([]byte, 20)...)
	resp, data := e.do("PUT", "/v1/me/avatar", a.Token, jpeg, "Content-Type", "image/png")
	e.wantError(resp, data, 400, "bad_image")
	resp, data = e.do("PUT", "/v1/me/avatar", a.Token, png1x1, "Content-Type", "image/gif")
	e.wantError(resp, data, 400, "bad_image")

	big := append([]byte("\x89PNG\r\n\x1a\n"), make([]byte, 70000)...)
	resp, data = e.do("PUT", "/v1/me/avatar", a.Token, big, "Content-Type", "image/png")
	e.wantError(resp, data, 413, "too_large")

	after := e.me(a.Token).AvatarHash
	if before == nil || after == nil || *before != *after {
		t.Fatalf("avatar changed: %v -> %v", before, after)
	}

	av, _, err := e.store.Avatar(context.Background(), a.User.ID)
	if err != nil || !bytes.Equal(av.Data, png1x1) {
		t.Fatalf("stored avatar changed: %v", err)
	}
}

func TestUserProfileRelation(t *testing.T) {
	e := newTestServer(t)
	a := e.native("dev1", "Вася")
	b := e.native("dev2", "Петя")

	resp, data := e.do("GET", fmt.Sprintf("/v1/users/%d", b.User.ID), a.Token, nil)
	if resp.StatusCode != 200 {
		t.Fatalf("%d %s", resp.StatusCode, data)
	}

	var got struct {
		testUser
		Friendship string `json:"friendship"`
		Blocked    string `json:"blocked"`
	}

	_ = json.Unmarshal(data, &got)

	if got.ID != b.User.ID || got.Name != "Петя" || got.Friendship != "none" || got.Blocked != "none" {
		t.Fatalf("%s", data)
	}

	resp, data = e.do("GET", "/v1/users/99999", a.Token, nil)
	e.wantError(resp, data, 404, "not_found")
	resp, data = e.do("GET", "/v1/users/abc", a.Token, nil)
	e.wantError(resp, data, 404, "not_found")
	resp, data = e.do("GET", fmt.Sprintf("/v1/users/%d", b.User.ID), "", nil)
	e.wantError(resp, data, 401, "unauthorized")
}

func TestStaticPages(t *testing.T) {
	e := newTestServer(t)
	for _, p := range []string{"/privacy-mobile.html", "/legal-mobile.txt"} {
		resp, data := e.do("GET", p, "", nil)
		if resp.StatusCode != 200 || len(data) == 0 {
			t.Fatalf("%s: %d", p, resp.StatusCode)
		}
	}
}

package api

import (
	"encoding/json"
	"fmt"
	"testing"
	"time"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

type friendRow struct {
	User   testUser `json:"user"`
	Status string   `json:"status"`
}

func (e *testEnv) friends(token string) []friendRow {
	e.t.Helper()

	resp, data := e.do("GET", "/v1/friends", token, nil)
	if resp.StatusCode != 200 {
		e.t.Fatalf("friends: %d %s", resp.StatusCode, data)
	}

	var out []friendRow
	if err := json.Unmarshal(data, &out); err != nil {
		e.t.Fatal(err)
	}

	return out
}

func (e *testEnv) wantStatus(method, path, token string, want int) {
	e.t.Helper()

	resp, data := e.do(method, path, token, nil)
	if resp.StatusCode != want {
		e.t.Fatalf("%s %s: %d, want %d: %s", method, path, resp.StatusCode, want, data)
	}
}

func TestFriendsEndToEnd(t *testing.T) {
	e := newTestServer(t)
	a, b := e.native("dev-a", "Alice"), e.native("dev-b", "Bob")
	pathB, pathA := fmt.Sprintf("/v1/friends/%d", b.User.ID), fmt.Sprintf("/v1/friends/%d", a.User.ID)

	e.wantStatus("PUT", pathB, a.Token, 204)

	got := e.friends(b.Token)
	if len(got) != 1 || got[0].Status != "incoming" || got[0].User.ID != a.User.ID {
		t.Fatalf("B friends: %+v", got)
	}

	if got := e.friends(a.Token); len(got) != 1 || got[0].Status != "outgoing" {
		t.Fatalf("A friends: %+v", got)
	}

	e.wantStatus("PUT", pathA, b.Token, 204)

	for _, tok := range []string{a.Token, b.Token} {
		if got := e.friends(tok); len(got) != 1 || got[0].Status != "friends" {
			t.Fatalf("friends: %+v", got)
		}
	}

	e.wantStatus("DELETE", pathB, a.Token, 204)

	for _, tok := range []string{a.Token, b.Token} {
		if got := e.friends(tok); len(got) != 0 {
			t.Fatalf("after delete: %+v", got)
		}
	}
}

func TestFriendErrors(t *testing.T) {
	e := newTestServer(t)
	a, b := e.native("dev-a", "Alice"), e.native("dev-b", "Bob")

	resp, data := e.do("PUT", fmt.Sprintf("/v1/friends/%d", a.User.ID), a.Token, nil)
	e.wantError(resp, data, 400, "self")

	resp, data = e.do("PUT", "/v1/friends/999999", a.Token, nil)
	e.wantError(resp, data, 404, "not_found")

	e.wantStatus("PUT", fmt.Sprintf("/v1/blocks/%d", b.User.ID), a.Token, 204)

	resp, data = e.do("PUT", fmt.Sprintf("/v1/friends/%d", b.User.ID), a.Token, nil)
	e.wantError(resp, data, 403, "blocked")

	resp, data = e.do("PUT", fmt.Sprintf("/v1/friends/%d", a.User.ID), b.Token, nil)
	e.wantError(resp, data, 403, "blocked")
}

func TestBlocksRemoveFriendship(t *testing.T) {
	e := newTestServer(t)
	a, b := e.native("dev-a", "Alice"), e.native("dev-b", "Bob")

	e.wantStatus("PUT", fmt.Sprintf("/v1/friends/%d", b.User.ID), a.Token, 204)
	e.wantStatus("PUT", fmt.Sprintf("/v1/friends/%d", a.User.ID), b.Token, 204)
	e.wantStatus("PUT", fmt.Sprintf("/v1/blocks/%d", b.User.ID), a.Token, 204)

	if got := e.friends(a.Token); len(got) != 0 {
		t.Fatalf("friends after block: %+v", got)
	}

	resp, data := e.do("GET", "/v1/blocks", a.Token, nil)
	if resp.StatusCode != 200 {
		t.Fatalf("blocks: %d %s", resp.StatusCode, data)
	}

	var blocks []testUser
	if err := json.Unmarshal(data, &blocks); err != nil || len(blocks) != 1 || blocks[0].ID != b.User.ID {
		t.Fatalf("blocks: %s", data)
	}

	e.wantStatus("DELETE", fmt.Sprintf("/v1/blocks/%d", b.User.ID), a.Token, 204)

	_, data = e.do("GET", "/v1/blocks", a.Token, nil)
	if string(data) != "[]\n" {
		t.Fatalf("blocks after unblock: %s", data)
	}
}

type historyResp struct {
	Games []struct {
		ID          int64    `json:"id"`
		Opponent    testUser `json:"opponent"`
		IsFriend    bool     `json:"isFriend"`
		StartedAt   int64    `json:"startedAt"`
		DurationSec int64    `json:"durationSec"`
		Words       int      `json:"words"`
	} `json:"games"`
	Next *int64 `json:"next"`
}

func (e *testEnv) history(token, query string) historyResp {
	e.t.Helper()

	resp, data := e.do("GET", "/v1/history"+query, token, nil)
	if resp.StatusCode != 200 {
		e.t.Fatalf("history: %d %s", resp.StatusCode, data)
	}

	var h historyResp
	if err := json.Unmarshal(data, &h); err != nil {
		e.t.Fatal(err)
	}

	return h
}

func TestHistoryPagination(t *testing.T) {
	e := newTestServer(t)
	a, b := e.native("dev-a", "Alice"), e.native("dev-b", "Bob")
	base := time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC)

	for i := range 60 {
		g := store.Game{
			StarterID: a.User.ID, InvitedID: b.User.ID,
			StartedAt: base.Add(time.Duration(i) * time.Minute), Duration: 90 * time.Second, Words: i,
		}
		if _, err := e.store.RecordGame(t.Context(), g); err != nil {
			t.Fatal(err)
		}
	}

	e.wantStatus("PUT", fmt.Sprintf("/v1/friends/%d", b.User.ID), a.Token, 204)
	e.wantStatus("PUT", fmt.Sprintf("/v1/friends/%d", a.User.ID), b.Token, 204)

	first := e.history(a.Token, "")
	if len(first.Games) != 50 || first.Next == nil || first.Games[0].Words != 59 {
		t.Fatalf("first page: %d games, next %v", len(first.Games), first.Next)
	}

	g0 := first.Games[0]
	if g0.Opponent.ID != b.User.ID || !g0.IsFriend || g0.DurationSec != 90 || g0.StartedAt != base.Add(59*time.Minute).UnixMilli() {
		t.Fatalf("game: %+v", g0)
	}

	second := e.history(a.Token, fmt.Sprintf("?before=%d", *first.Next))
	if len(second.Games) != 10 || second.Next != nil {
		t.Fatalf("second page: %d games, next %v", len(second.Games), second.Next)
	}

	if got := e.history(a.Token, fmt.Sprintf("?with=%d", a.User.ID+100)); len(got.Games) != 0 {
		t.Fatalf("with filter: %d games", len(got.Games))
	}

	resp, data := e.do("GET", "/v1/history?before=x", a.Token, nil)
	e.wantError(resp, data, 400, "bad_request")
}

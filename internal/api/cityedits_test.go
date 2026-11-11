package api

import (
	"encoding/json"
	"fmt"
	"strings"
	"testing"
)

func TestCityEditLimits(t *testing.T) {
	e := newTestServer(t)
	a := e.native("dev-a", "Alice")

	resp, data := e.do("POST", "/v1/city-edits", a.Token, map[string]any{
		"newName": "Город", "reason": strings.Repeat("я", 121),
	})
	e.wantError(resp, data, 400, "bad_reason")

	for _, field := range []string{"oldName", "newName"} {
		resp, data = e.do("POST", "/v1/city-edits", a.Token, map[string]any{field: strings.Repeat("я", 101)})
		e.wantError(resp, data, 400, "bad_request")
	}

	b := e.native("dev-b", "Bob")
	long := map[string]any{"oldName": strings.Repeat("я", 100), "newName": strings.Repeat("я", 100)}

	if resp, data := e.do("POST", "/v1/city-edits", b.Token, long); resp.StatusCode != 201 {
		t.Fatalf("100-character names: %d %s", resp.StatusCode, data)
	}

	ok := map[string]any{"oldCountryCode": 1, "newCountryCode": 2, "oldName": "Старый", "reason": strings.Repeat("я", 120)}

	for i := range 50 {
		if resp, data := e.do("POST", "/v1/city-edits", a.Token, ok); resp.StatusCode != 201 {
			t.Fatalf("edit %d: %d %s", i, resp.StatusCode, data)
		}
	}

	resp, data = e.do("POST", "/v1/city-edits", a.Token, ok)
	e.wantError(resp, data, 429, "too_many_requests")

	resp, data = e.do("GET", "/v1/city-edits", a.Token, nil)
	if resp.StatusCode != 200 {
		t.Fatalf("list: %d %s", resp.StatusCode, data)
	}

	var list []map[string]any
	if err := json.Unmarshal(data, &list); err != nil || len(list) != 50 {
		t.Fatalf("list: %d entries, err %v", len(list), err)
	}

	first := list[0]
	if first["status"] != "NEW" || first["newName"] != nil || first["oldName"] != "Старый" || first["oldCountryCode"] != float64(1) {
		t.Fatalf("entry: %v", first)
	}
}

func TestCityEditWithdraw(t *testing.T) {
	e := newTestServer(t)
	a := e.native("dev-a", "Alice")
	b := e.native("dev-b", "Bob")

	resp, data := e.do("POST", "/v1/city-edits", a.Token, map[string]any{"newName": "Город"})
	if resp.StatusCode != 201 {
		t.Fatalf("create: %d %s", resp.StatusCode, data)
	}

	var created struct {
		ID int64 `json:"id"`
	}
	if err := json.Unmarshal(data, &created); err != nil {
		t.Fatal(err)
	}

	path := fmt.Sprintf("/v1/city-edits/%d", created.ID)

	resp, data = e.do("DELETE", path, b.Token, nil)
	e.wantError(resp, data, 404, "not_found")

	resp, data = e.do("DELETE", "/v1/city-edits/abc", a.Token, nil)
	e.wantError(resp, data, 404, "not_found")

	e.wantStatus("DELETE", path, a.Token, 204)

	resp, data = e.do("DELETE", path, a.Token, nil)
	e.wantError(resp, data, 404, "not_found")

	resp, data = e.do("GET", "/v1/city-edits", a.Token, nil)
	if resp.StatusCode != 200 || strings.TrimSpace(string(data)) != "[]" {
		t.Fatalf("list after withdraw: %d %s", resp.StatusCode, data)
	}
}

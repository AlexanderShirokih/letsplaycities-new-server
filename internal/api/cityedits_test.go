package api

import (
	"encoding/json"
	"strings"
	"testing"
)

func TestCityEditLimits(t *testing.T) {
	e := newTestServer(t)
	a := e.native("dev-a", "Alice")

	resp, data := e.do("POST", "/v1/city-edits", a.Token, map[string]any{
		"newName": "Город", "reason": strings.Repeat("я", 65),
	})
	e.wantError(resp, data, 400, "bad_reason")

	ok := map[string]any{"oldCountryCode": 1, "newCountryCode": 2, "oldName": "Старый", "reason": strings.Repeat("я", 64)}

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

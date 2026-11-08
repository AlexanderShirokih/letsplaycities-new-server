package store

import (
	"context"
	"testing"
)

func TestCityEdits(t *testing.T) {
	s := openMem(t)
	ctx := context.Background()
	id := mkUsers(t, s, "A")[0]
	str := func(v string) *string { return &v }

	var ids []int64

	for i, e := range []CityEdit{
		{OldCountry: 1, NewCountry: 2, OldName: str("Москва"), NewName: str("Москва2"), Reason: str("опечатка")},
		{NewCountry: 3, NewName: str("Новый")},
		{OldCountry: 4, OldName: str("Старый")},
	} {
		eid, err := s.AddCityEdit(ctx, id, e)
		if err != nil {
			t.Fatalf("%d: %v", i, err)
		}

		ids = append(ids, eid)
	}

	n, err := s.CountNewCityEdits(ctx, id)
	if err != nil || n != 3 {
		t.Fatalf("count %d %v", n, err)
	}

	list, err := s.CityEdits(ctx, id, 50)
	if err != nil || len(list) != 3 {
		t.Fatalf("list %d %v", len(list), err)
	}

	if list[0].ID != ids[2] || list[2].ID != ids[0] {
		t.Fatalf("order: %+v", list)
	}

	if list[0].Status != "NEW" || list[0].NewName != nil || list[0].OldName == nil || *list[0].OldName != "Старый" {
		t.Fatalf("entry: %+v", list[0])
	}

	if list[2].Reason == nil || *list[2].Reason != "опечатка" || list[2].Verdict != nil || list[2].NewCountry != 2 {
		t.Fatalf("entry: %+v", list[2])
	}

	if l, _ := s.CityEdits(ctx, id, 2); len(l) != 2 {
		t.Fatalf("limit: %d", len(l))
	}
}

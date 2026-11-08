package geo_test

import (
	"math"
	"os"
	"testing"

	"lps/internal/dict"
	"lps/internal/geo"
)

func haversine(a, b geo.Point) float64 {
	const r = 6371.0

	rad := math.Pi / 180
	dLat, dLon := (b.Lat-a.Lat)*rad, (b.Lon-a.Lon)*rad
	h := math.Pow(math.Sin(dLat/2), 2) + math.Cos(a.Lat*rad)*math.Cos(b.Lat*rad)*math.Pow(math.Sin(dLon/2), 2)

	return 2 * r * math.Asin(math.Sqrt(h))
}

func TestRealV81Distances(t *testing.T) {
	df, err := os.Open("../../testdata/dict/data-81.db2")
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = df.Close() }()

	d, err := dict.Parse(df)
	if err != nil {
		t.Fatal(err)
	}

	gf, err := os.Open("../../testdata/dict/geo-81.bin")
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = gf.Close() }()

	ver, pts, err := geo.Read(gf)
	if err != nil {
		t.Fatal(err)
	}

	cities := d.Cities()
	if int(ver) != d.Version || len(pts) != len(cities) {
		t.Fatalf("geo v%d with %d points, dictionary v%d with %d cities", ver, len(pts), d.Version, len(cities))
	}

	at := make(map[string]geo.Point, len(cities))
	for i, c := range cities {
		at[c.Name] = pts[i]
	}

	for _, tc := range []struct {
		a, b string
		km   float64
	}{
		{"москва", "санкт-петербург", 634},
		{"сантьяго", "осака", 17607},
		{"мадрид", "кадис", 487},
	} {
		got := haversine(at[tc.a], at[tc.b])
		if math.Abs(got-tc.km)/tc.km > 0.01 {
			t.Errorf("%s — %s = %.0f km, want %.0f", tc.a, tc.b, got, tc.km)
		}
	}
}

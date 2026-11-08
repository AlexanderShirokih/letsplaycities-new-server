package main

import (
	"bytes"
	"math"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"testing"

	"lps/internal/dict"
	"lps/internal/geo"
)

const dictPath = "../../testdata/dict/data-80.db2"

func loadCities(t *testing.T) []dict.City {
	t.Helper()

	f, err := os.Open(dictPath)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = f.Close() }()

	d, err := dict.Parse(f)
	if err != nil {
		t.Fatal(err)
	}

	return d.Cities()
}

// writeTable writes a cities-geo.tsv with a synthetic point per city, skipping skip.
func writeTable(t *testing.T, cities []dict.City, skip string, extra string) string {
	t.Helper()

	var b strings.Builder

	b.WriteString("name\tcountry_id\tlat\tlon\tsource\tref\tpopulation\n")

	for i, c := range cities {
		if c.Name == skip {
			continue
		}

		lat := float64(i%180) - 89.5
		lon := float64(i%360) - 179.5
		b.WriteString(strings.Join([]string{c.Name, itoa(int(c.Country)), ftoa(lat), ftoa(lon), "geonames", "1", "1000"}, "\t") + "\n")
	}

	b.WriteString(extra)

	path := filepath.Join(t.TempDir(), "cities-geo.tsv")
	if err := os.WriteFile(path, []byte(b.String()), 0o644); err != nil {
		t.Fatal(err)
	}

	return path
}

func TestBuild(t *testing.T) {
	cities := loadCities(t)
	table := writeTable(t, cities, "", "несуществующий\t0\t1.00000\t2.00000\tweb\tx\t\n")
	out := filepath.Join(t.TempDir(), "geo-80.bin")

	var stdout, stderr bytes.Buffer
	if code := run([]string{"-dict", dictPath, "-cities", table, "-out", out}, &stdout, &stderr); code != exitOK {
		t.Fatalf("exit %d: %s", code, stderr.String())
	}

	if !strings.Contains(stderr.String(), "несуществующий") {
		t.Errorf("no warning about extra row: %q", stderr.String())
	}

	f, err := os.Open(out)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = f.Close() }()

	ver, pts, err := geo.Read(f)
	if err != nil {
		t.Fatal(err)
	}

	if ver != 80 || len(pts) != len(cities) {
		t.Fatalf("ver = %d, len = %d", ver, len(pts))
	}

	for _, i := range []int{0, 1, len(cities) / 2, len(cities) - 1} {
		want := geo.Point{Lat: float64(i%180) - 89.5, Lon: float64(i%360) - 179.5}
		if math.Abs(pts[i].Lat-want.Lat) > 1e-4 || math.Abs(pts[i].Lon-want.Lon) > 1e-4 {
			t.Errorf("point %d = %+v, want %+v", i, pts[i], want)
		}
	}
}

func TestBuildFailsOnMissingCity(t *testing.T) {
	cities := loadCities(t)
	table := writeTable(t, cities, "москва", "")
	out := filepath.Join(t.TempDir(), "geo-80.bin")

	var stdout, stderr bytes.Buffer
	if code := run([]string{"-dict", dictPath, "-cities", table, "-out", out}, &stdout, &stderr); code != exitError {
		t.Fatalf("exit %d", code)
	}

	if !strings.Contains(stderr.String(), "москва") {
		t.Errorf("missing city not reported: %q", stderr.String())
	}

	if _, err := os.Stat(out); !os.IsNotExist(err) {
		t.Error("output written despite missing city")
	}
}

func TestBuildRejectsBadRow(t *testing.T) {
	cities := loadCities(t)

	for name, row := range map[string]string{
		"columns":   "москва\t51\t55.7\n",
		"latitude":  "москва\t51\tabc\t37.6\tweb\tx\t\n",
		"duplicate": "москва\t51\t55.7\t37.6\tweb\tx\t\n",
	} {
		table := writeTable(t, cities, "", row)

		var stdout, stderr bytes.Buffer
		if code := run([]string{"-dict", dictPath, "-cities", table, "-out", filepath.Join(t.TempDir(), "g.bin")}, &stdout, &stderr); code != exitError {
			t.Errorf("%s: exit %d", name, code)
		}
	}
}

func itoa(i int) string { return strconv.Itoa(i) }

func ftoa(f float64) string { return strconv.FormatFloat(f, 'f', 5, 64) }

func TestBuildRejectsHeaderWithoutCoordinates(t *testing.T) {
	path := filepath.Join(t.TempDir(), "cities-geo.tsv")
	if err := os.WriteFile(path, []byte("name\tlatitude\tlon\nмосква\t55.7\t37.6\n"), 0o644); err != nil {
		t.Fatal(err)
	}

	var stdout, stderr bytes.Buffer
	if code := run([]string{"-dict", dictPath, "-cities", path, "-out", filepath.Join(t.TempDir(), "g.bin")}, &stdout, &stderr); code != exitError {
		t.Fatalf("exit %d", code)
	}

	if !strings.Contains(stderr.String(), "lat") {
		t.Errorf("missing column not reported: %q", stderr.String())
	}
}

package geo

import (
	"bytes"
	"math"
	"testing"
)

func TestRoundTrip(t *testing.T) {
	pts := []Point{{55.75583, 37.61730}, {-33.44889, -70.66926}, {90, 180}, {-90, -180}, {0, 0}, {64.5, -179.99999}}

	var buf bytes.Buffer
	if err := Write(&buf, 80, pts); err != nil {
		t.Fatal(err)
	}

	if want := 12 + 6*len(pts) + 4; buf.Len() != want {
		t.Fatalf("size = %d, want %d", buf.Len(), want)
	}

	ver, got, err := Read(bytes.NewReader(buf.Bytes()))
	if err != nil {
		t.Fatal(err)
	}

	if ver != 80 || len(got) != len(pts) {
		t.Fatalf("ver = %d, len = %d", ver, len(got))
	}

	for i, p := range pts {
		if math.Abs(got[i].Lat-p.Lat) > 1e-4 || math.Abs(got[i].Lon-p.Lon) > 1e-4 {
			t.Errorf("point %d = %+v, want %+v", i, got[i], p)
		}
	}
}

func TestWriteRejectsOutOfRange(t *testing.T) {
	for _, p := range []Point{{91, 0}, {0, 181}, {math.NaN(), 0}} {
		if err := Write(&bytes.Buffer{}, 1, []Point{p}); err == nil {
			t.Errorf("%+v accepted", p)
		}
	}
}

func TestReadRejects(t *testing.T) {
	var buf bytes.Buffer
	if err := Write(&buf, 7, []Point{{1, 2}, {3, 4}}); err != nil {
		t.Fatal(err)
	}

	good := buf.Bytes()

	corrupt := func(f func(b []byte) []byte) []byte {
		return f(append([]byte(nil), good...))
	}

	cases := map[string][]byte{
		"magic":     corrupt(func(b []byte) []byte { b[0] ^= 1; return b }),
		"format":    corrupt(func(b []byte) []byte { b[3] = 2; return b }),
		"crc":       corrupt(func(b []byte) []byte { b[13] ^= 1; return b }),
		"truncated": good[:len(good)-1],
		"trailing":  append(append([]byte(nil), good...), 0),
		"count":     corrupt(func(b []byte) []byte { b[11] = 3; return b }),
	}

	for name, data := range cases {
		if _, _, err := Read(bytes.NewReader(data)); err == nil {
			t.Errorf("%s: accepted", name)
		}
	}
}

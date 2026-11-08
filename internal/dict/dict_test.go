package dict

import (
	"bytes"
	"encoding/binary"
	"math/rand"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"testing"
)

func encodeName(name string) []byte {
	var out []byte

	for _, r := range name {
		if r < 127 {
			out = append(out, byte(r))
		} else {
			out = append(out, byte(r-(0x0430-127)))
		}
	}

	return out
}

func record(buf *bytes.Buffer, name string, diff uint8, country uint16) {
	enc := encodeName(name)
	buf.WriteByte(byte(len(enc)))
	buf.WriteByte(diff)
	_ = binary.Write(buf, binary.BigEndian, country)
	buf.Write(enc)
}

func buildDB(t *testing.T, version uint32, cities []City) []byte {
	t.Helper()

	var buf bytes.Buffer

	_ = binary.Write(&buf, binary.BigEndian, uint16(0xFED0))
	_ = binary.Write(&buf, binary.BigEndian, uint16(1))
	_ = binary.Write(&buf, binary.BigEndian, version)

	_ = binary.Write(&buf, binary.BigEndian, uint32(len(cities)))
	for _, c := range cities {
		record(&buf, c.Name, c.Diff, c.Country)
	}

	record(&buf, strconv.Itoa(len(cities))+"abcdef", 0, 0)

	return buf.Bytes()
}

func TestParseRealV80(t *testing.T) {
	f, err := os.Open("../../testdata/dict/data-80.db2")
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = f.Close() }()

	d, err := Parse(f)
	if err != nil {
		t.Fatal(err)
	}

	if d.Version != 80 {
		t.Fatalf("version = %d", d.Version)
	}

	if !d.Contains("москва") {
		t.Error("москва missing")
	}

	if d.Contains("абвгд") {
		t.Error("абвгд must not exist")
	}
}

func TestParseRejects(t *testing.T) {
	good := buildDB(t, 1, []City{{"москва", 0, 1}, {"омск", 1, 1}})
	if _, err := Parse(bytes.NewReader(good)); err != nil {
		t.Fatalf("good rejected: %v", err)
	}

	mut := func(f func(b []byte)) []byte {
		b := append([]byte(nil), good...)
		f(b)

		return b
	}

	cases := map[string][]byte{
		"bad magic":    mut(func(b []byte) { b[0] = 0 }),
		"bad settings": mut(func(b []byte) { b[3] = 2 }),
		"truncated":    good[:len(good)-3],
		"header only":  good[:6],
		"empty":        nil,
		"bad check": mut(func(b []byte) { // count claims 3 but 2 records + check
			b[11] = 3
		}),
		"bad check text": mut(func(b []byte) { b[len(b)-7] = byte('9') }),
	}
	for name, data := range cases {
		if _, err := Parse(bytes.NewReader(data)); err == nil {
			t.Errorf("%s: expected error", name)
		}
	}
}

func TestLoadLatestPicksMaxN(t *testing.T) {
	dir := t.TempDir()
	if _, _, err := LoadLatest(dir); err == nil {
		t.Fatal("empty dir must fail")
	}

	for _, n := range []uint32{45, 80} {
		p := filepath.Join(dir, "data-"+strconv.Itoa(int(n))+".db2")
		if err := os.WriteFile(p, buildDB(t, n, []City{{"омск", 0, 1}}), 0o644); err != nil {
			t.Fatal(err)
		}
	}

	_ = os.WriteFile(filepath.Join(dir, "data-999.txt"), []byte("x"), 0o644)
	_ = os.WriteFile(filepath.Join(dir, "data-x.db2"), []byte("x"), 0o644)

	d, p, err := LoadLatest(dir)
	if err != nil {
		t.Fatal(err)
	}

	if filepath.Base(p) != "data-80.db2" || d.Version != 80 {
		t.Fatalf("got %s v%d", p, d.Version)
	}
}

func TestPick(t *testing.T) {
	cities := []City{
		{"астана", 0, 7}, {"анапа", 1, 1}, {"аккра", 3, 9}, {"омск", 0, 1},
	}

	d, err := Parse(bytes.NewReader(buildDB(t, 1, cities)))
	if err != nil {
		t.Fatal(err)
	}

	rnd := rand.New(rand.NewSource(1))
	for i := 0; i < 50; i++ {
		w, ok := d.Pick('а', 3, nil, func(string) bool { return false }, rnd)
		if !ok || w[:2] != "а"[:2] {
			t.Fatalf("got %q %v", w, ok)
		}
	}
	// maxDiff excludes аккра
	for i := 0; i < 50; i++ {
		w, _ := d.Pick('а', 1, nil, func(string) bool { return false }, rnd)
		if w == "аккра" {
			t.Fatal("diff not respected")
		}
	}
	// used excludes
	used := map[string]bool{"астана": true, "анапа": true}

	for i := 0; i < 50; i++ {
		w, ok := d.Pick('а', 3, nil, func(s string) bool { return used[s] }, rnd)
		if !ok || w != "аккра" {
			t.Fatalf("got %q", w)
		}
	}
	// everything used
	if _, ok := d.Pick('а', 3, nil, func(string) bool { return true }, rnd); ok {
		t.Fatal("expected false")
	}
	// no such letter
	if _, ok := d.Pick('я', 3, nil, func(string) bool { return false }, rnd); ok {
		t.Fatal("expected false")
	}
	// country preference
	n7 := 0

	for i := 0; i < 1000; i++ {
		w, _ := d.Pick('а', 3, []uint16{7}, func(string) bool { return false }, rnd)
		if w == "астана" {
			n7++
		}
	}

	if n7 < 650 {
		t.Fatalf("preferred country picked only %d/1000", n7)
	}
	// preferred country with no candidate falls back
	if _, ok := d.Pick('а', 3, []uint16{42}, func(string) bool { return false }, rnd); !ok {
		t.Fatal("fallback failed")
	}
}

func TestCitiesKeepFileOrder(t *testing.T) {
	cities := []City{{Name: "омск", Diff: 1, Country: 51}, {Name: "анкара", Country: 109}, {Name: "бонн", Diff: 2, Country: 118}}

	d, err := Parse(bytes.NewReader(buildDB(t, 1, cities)))
	if err != nil {
		t.Fatal(err)
	}

	got := d.Cities()
	if len(got) != len(cities) {
		t.Fatalf("len = %d", len(got))
	}

	for i := range cities {
		if got[i] != cities[i] {
			t.Errorf("Cities()[%d] = %+v, want %+v", i, got[i], cities[i])
		}
	}
}

func TestWriteReproducesV80(t *testing.T) {
	orig, err := os.ReadFile("../../testdata/dict/data-80.db2")
	if err != nil {
		t.Fatal(err)
	}

	d, err := Parse(bytes.NewReader(orig))
	if err != nil {
		t.Fatal(err)
	}

	var buf bytes.Buffer
	if err := Write(&buf, 80, d.Cities()); err != nil {
		t.Fatal(err)
	}

	got := buf.Bytes()
	if len(got) != len(orig) {
		t.Fatalf("size = %d, want %d", len(got), len(orig))
	}

	// The trailer's 6-character suffix is arbitrary.
	if body := len(orig) - checkSuffix; !bytes.Equal(got[:body], orig[:body]) {
		t.Fatal("records differ from the original file")
	}
}

func TestWriteRejectsBadCity(t *testing.T) {
	for _, c := range []City{{Name: ""}, {Name: "москва", Diff: 4}, {Name: "Москва"}, {Name: strings.Repeat("а", 256)}} {
		if err := Write(&bytes.Buffer{}, 1, []City{c}); err == nil {
			t.Errorf("%+v accepted", c)
		}
	}
}

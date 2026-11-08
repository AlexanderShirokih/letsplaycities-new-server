package main

import (
	"bytes"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"lps/internal/dict"
)

const src = "../../testdata/dict/data-80.db2"

func writeDrop(t *testing.T, lines string) string {
	t.Helper()

	path := filepath.Join(t.TempDir(), "drop.txt")
	if err := os.WriteFile(path, []byte(lines), 0o644); err != nil {
		t.Fatal(err)
	}

	return path
}

func loadDict(t *testing.T, path string) *dict.Dictionary {
	t.Helper()

	f, err := os.Open(path)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = f.Close() }()

	d, err := dict.Parse(f)
	if err != nil {
		t.Fatal(err)
	}

	return d
}

func TestDrop(t *testing.T) {
	out := filepath.Join(t.TempDir(), "data-81.db2")
	drop := writeDrop(t, "# name\tcountry\treason\nграфство\tНеизвестно\tне город\n\nбуг\n")

	var stdout, stderr bytes.Buffer
	if code := run([]string{"-in", src, "-drop", drop, "-version", "81", "-out", out}, &stdout, &stderr); code != exitOK {
		t.Fatalf("exit %d: %s", code, stderr.String())
	}

	before, after := loadDict(t, src), loadDict(t, out)

	if after.Version != 81 || len(after.Cities()) != len(before.Cities())-2 {
		t.Fatalf("version %d, %d cities", after.Version, len(after.Cities()))
	}

	if after.Contains("графство") || after.Contains("буг") || !after.Contains("москва") {
		t.Error("wrong cities dropped")
	}
}

func TestDropRejects(t *testing.T) {
	cases := map[string][]string{
		"unknown city":  {"-drop", writeDrop(t, "абвгд\n"), "-version", "81"},
		"same version":  {"-drop", writeDrop(t, "буг\n"), "-version", "80"},
		"older version": {"-drop", writeDrop(t, "буг\n"), "-version", "79"},
	}

	for name, args := range cases {
		out := filepath.Join(t.TempDir(), "d.db2")

		var stdout, stderr bytes.Buffer
		if code := run(append([]string{"-in", src, "-out", out}, args...), &stdout, &stderr); code != exitError {
			t.Errorf("%s: exit %d", name, code)
		}

		if _, err := os.Stat(out); !os.IsNotExist(err) {
			t.Errorf("%s: output written", name)
		}

		if name == "unknown city" && !strings.Contains(stderr.String(), "абвгд") {
			t.Errorf("unknown city not reported: %q", stderr.String())
		}
	}
}

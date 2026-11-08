// Command lps-geo builds geo-N.bin from a dictionary and the cities-geo.tsv table.
//
//	lps-geo -dict data-80.db2 -cities geodata/cities-geo.tsv -out geo-80.bin
//
// The table is tab-separated with a header row. lps-geo reads the name, lat and lon columns
// and ignores the rest. Every dictionary city needs a row; rows for cities missing from the
// dictionary are reported as warnings.
//
// Exit codes: 0 built, 2 usage or data error.
package main

import (
	"bufio"
	"bytes"
	"flag"
	"fmt"
	"io"
	"os"
	"strconv"
	"strings"

	"lps/internal/dict"
	"lps/internal/geo"
)

const (
	exitOK    = 0
	exitError = 2
)

// required are the table columns lps-geo reads.
var required = []string{"name", "lat", "lon"}

func main() {
	os.Exit(run(os.Args[1:], os.Stdout, os.Stderr))
}

func run(args []string, stdout, stderr io.Writer) int {
	fs := flag.NewFlagSet("lps-geo", flag.ContinueOnError)
	fs.SetOutput(stderr)

	dictPath := fs.String("dict", "", "dictionary (data-N.db2)")
	citiesPath := fs.String("cities", "", "coordinates table (cities-geo.tsv)")
	out := fs.String("out", "", "file to write (geo-N.bin)")

	if err := fs.Parse(args); err != nil {
		return exitError
	}

	if *dictPath == "" || *citiesPath == "" || *out == "" || fs.NArg() > 0 {
		_, _ = fmt.Fprintln(stderr, "usage: lps-geo -dict data-N.db2 -cities cities-geo.tsv -out geo-N.bin")

		return exitError
	}

	if err := build(*dictPath, *citiesPath, *out, stdout, stderr); err != nil {
		_, _ = fmt.Fprintln(stderr, "lps-geo:", err)

		return exitError
	}

	return exitOK
}

func build(dictPath, citiesPath, out string, stdout, stderr io.Writer) error {
	d, err := loadDict(dictPath)
	if err != nil {
		return err
	}

	table, err := loadTable(citiesPath)
	if err != nil {
		return err
	}

	cities := d.Cities()
	pts := make([]geo.Point, len(cities))

	var missing []string

	for i, c := range cities {
		p, ok := table[c.Name]
		if !ok {
			missing = append(missing, c.Name)

			continue
		}

		pts[i] = p

		delete(table, c.Name)
	}

	if len(missing) > 0 {
		return fmt.Errorf("%d cities have no coordinates: %s", len(missing), strings.Join(missing, ", "))
	}

	for name := range table {
		_, _ = fmt.Fprintf(stderr, "lps-geo: warning: %q is not in the dictionary\n", name)
	}

	var buf bytes.Buffer
	if err := geo.Write(&buf, uint32(d.Version), pts); err != nil {
		return err
	}

	if err := os.WriteFile(out, buf.Bytes(), 0o644); err != nil {
		return err
	}

	_, _ = fmt.Fprintf(stdout, "%s: %d cities, dictionary v%d\n", out, len(pts), d.Version)

	return nil
}

func loadDict(path string) (*dict.Dictionary, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = f.Close() }()

	return dict.Parse(f)
}

// loadTable reads cities-geo.tsv into name → point.
func loadTable(path string) (map[string]geo.Point, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = f.Close() }()

	sc := bufio.NewScanner(f)
	if !sc.Scan() {
		return nil, fmt.Errorf("%s: no header", path)
	}

	header := strings.Split(sc.Text(), "\t")

	col, err := columnIndex(header)
	if err != nil {
		return nil, fmt.Errorf("%s: %w", path, err)
	}

	table := make(map[string]geo.Point)

	for line := 2; sc.Scan(); line++ {
		if sc.Text() == "" {
			continue
		}

		name, p, err := parseRow(sc.Text(), len(header), col)
		if err != nil {
			return nil, fmt.Errorf("%s:%d: %w", path, line, err)
		}

		if _, dup := table[name]; dup {
			return nil, fmt.Errorf("%s:%d: duplicate city %q", path, line, name)
		}

		table[name] = p
	}

	return table, sc.Err()
}

// columnIndex maps each required column to its position in header.
func columnIndex(header []string) (map[string]int, error) {
	col := make(map[string]int)

	for i, h := range header {
		col[h] = i
	}

	for _, name := range required {
		if _, ok := col[name]; !ok {
			return nil, fmt.Errorf("header has no %q column", name)
		}
	}

	return col, nil
}

func parseRow(row string, width int, col map[string]int) (string, geo.Point, error) {
	f := strings.Split(row, "\t")
	if len(f) != width {
		return "", geo.Point{}, fmt.Errorf("want %d columns, got %d", width, len(f))
	}

	lat, err := strconv.ParseFloat(f[col["lat"]], 64)
	if err != nil {
		return "", geo.Point{}, fmt.Errorf("lat: %w", err)
	}

	lon, err := strconv.ParseFloat(f[col["lon"]], 64)
	if err != nil {
		return "", geo.Point{}, fmt.Errorf("lon: %w", err)
	}

	return f[col["name"]], geo.Point{Lat: lat, Lon: lon}, nil
}

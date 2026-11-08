// Package dict reads the .db2 city dictionary.
package dict

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"math/rand"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"unicode/utf8"
)

const (
	magic        = 0xFED0
	settingsV1   = 0x0001
	checkSuffix  = 6
	cyrillicBase = 0x0430 - 127
	preferProb   = 0.7
)

// City holds the properties of one dictionary entry.
type City struct {
	Name    string
	Diff    uint8
	Country uint16
}

// Dictionary is an in-memory city dictionary.
type Dictionary struct {
	Version int

	cities  map[string]City
	byRune  map[rune][]string
	ordered []City
}

// Parse reads a .db2 stream.
func Parse(r io.Reader) (*Dictionary, error) {
	br := bufio.NewReader(r)

	version, count, err := readHeader(br)
	if err != nil {
		return nil, err
	}

	d := &Dictionary{
		Version: int(version),
		cities:  make(map[string]City),
		byRune:  make(map[rune][]string),
	}

	for i := uint32(0); i < count; i++ {
		rh, name, err := readRecord(br, i)
		if err != nil {
			return nil, err
		}

		if name == "" {
			return nil, fmt.Errorf("dict: record %d has empty name", i)
		}

		d.add(City{Name: name, Diff: rh[1] & 3, Country: binary.BigEndian.Uint16(rh[2:])})
	}

	if err := checkTrailer(br, count); err != nil {
		return nil, err
	}

	return d, nil
}

func readHeader(br *bufio.Reader) (version, count uint32, err error) {
	var hdr [12]byte
	if _, err := io.ReadFull(br, hdr[:]); err != nil {
		return 0, 0, fmt.Errorf("dict: read header: %w", err)
	}

	if m := binary.BigEndian.Uint16(hdr[0:]); m != magic {
		return 0, 0, fmt.Errorf("dict: invalid magic %#x", m)
	}

	if s := binary.BigEndian.Uint16(hdr[2:]); s != settingsV1 {
		return 0, 0, fmt.Errorf("dict: unsupported settings %d", s)
	}

	return binary.BigEndian.Uint32(hdr[4:]), binary.BigEndian.Uint32(hdr[8:]), nil
}

// readRecord reads record i: its 4-byte header and decoded name.
func readRecord(br *bufio.Reader, i uint32) (rh [4]byte, name string, err error) {
	if _, err := io.ReadFull(br, rh[:]); err != nil {
		return rh, "", fmt.Errorf("dict: record %d header: %w", i, err)
	}

	raw := make([]byte, rh[0])
	if _, err := io.ReadFull(br, raw); err != nil {
		return rh, "", fmt.Errorf("dict: record %d name: %w", i, err)
	}

	return rh, decodeName(raw), nil
}

// checkTrailer reads the record after the last city: its name must start with the city count.
func checkTrailer(br *bufio.Reader, count uint32) error {
	_, name, err := readRecord(br, count)
	if err != nil {
		return err
	}

	if len(name) < checkSuffix || name[:len(name)-checkSuffix] != strconv.FormatUint(uint64(count), 10) {
		return errors.New("dict: file check failed")
	}

	return nil
}

func (d *Dictionary) add(c City) {
	d.ordered = append(d.ordered, c)

	if _, dup := d.cities[c.Name]; !dup {
		first, _ := utf8.DecodeRuneInString(c.Name)
		d.byRune[first] = append(d.byRune[first], c.Name)
	}

	d.cities[c.Name] = c
}

func decodeName(raw []byte) string {
	runes := make([]rune, len(raw))
	for i, b := range raw {
		if b < 127 {
			runes[i] = rune(b)
		} else {
			runes[i] = rune(b) + cyrillicBase
		}
	}

	return string(runes)
}

var fileRe = regexp.MustCompile(`^data-(\d+)\.db2$`)

// LoadLatest parses the data-N.db2 file with the largest N in dir.
func LoadLatest(dir string) (*Dictionary, string, error) {
	name, err := latestFile(dir)
	if err != nil {
		return nil, "", err
	}

	path := filepath.Join(dir, name)

	f, err := os.Open(path)
	if err != nil {
		return nil, "", err
	}
	defer func() { _ = f.Close() }()

	d, err := Parse(f)
	if err != nil {
		return nil, "", fmt.Errorf("%s: %w", path, err)
	}

	return d, path, nil
}

// latestFile returns the name of the data-N.db2 file with the largest N in dir.
func latestFile(dir string) (string, error) {
	entries, err := os.ReadDir(dir)
	if err != nil {
		return "", err
	}

	best, bestN := "", -1

	for _, e := range entries {
		if e.IsDir() {
			continue
		}

		if n, ok := fileNumber(e.Name()); ok && n > bestN {
			best, bestN = e.Name(), n
		}
	}

	if bestN < 0 {
		return "", fmt.Errorf("dict: no data-N.db2 files in %s", dir)
	}

	return best, nil
}

// fileNumber extracts N from a data-N.db2 file name.
func fileNumber(name string) (int, bool) {
	m := fileRe.FindStringSubmatch(name)
	if m == nil {
		return 0, false
	}

	n, err := strconv.Atoi(m[1])

	return n, err == nil
}

// trailerSuffix pads the check record; readers ignore its value.
const trailerSuffix = "lpsgeo"

// Write encodes cities as a .db2 file with the given version.
func Write(w io.Writer, version uint32, cities []City) error {
	var buf bytes.Buffer

	_ = binary.Write(&buf, binary.BigEndian, struct {
		Magic, Settings uint16
		Version, Count  uint32
	}{magic, settingsV1, version, uint32(len(cities))})

	for i, c := range cities {
		if c.Diff > 3 {
			return fmt.Errorf("dict: city %d %q: diff %d out of range", i, c.Name, c.Diff)
		}

		if err := writeRecord(&buf, c.Name, c.Diff, c.Country); err != nil {
			return fmt.Errorf("dict: city %d: %w", i, err)
		}
	}

	if err := writeRecord(&buf, strconv.Itoa(len(cities))+trailerSuffix, 0xFF, 0); err != nil {
		return err
	}

	_, err := w.Write(buf.Bytes())

	return err
}

func writeRecord(buf *bytes.Buffer, name string, diff uint8, country uint16) error {
	raw, err := encodeRaw(name)
	if err != nil {
		return err
	}

	buf.WriteByte(byte(len(raw)))
	buf.WriteByte(diff)
	_ = binary.Write(buf, binary.BigEndian, country)
	buf.Write(raw)

	return nil
}

func encodeRaw(name string) ([]byte, error) {
	var raw []byte

	for _, r := range name {
		switch {
		case r < 127:
			raw = append(raw, byte(r))
		case r >= 127+cyrillicBase && r <= 255+cyrillicBase:
			raw = append(raw, byte(r-cyrillicBase))
		default:
			return nil, fmt.Errorf("%q: rune %q cannot be encoded", name, r)
		}
	}

	if len(raw) == 0 || len(raw) > 255 {
		return nil, fmt.Errorf("%q: encoded length %d out of range", name, len(raw))
	}

	return raw, nil
}

// Cities returns all records in file order.
func (d *Dictionary) Cities() []City {
	return append([]City(nil), d.ordered...)
}

// Contains reports whether word is in the dictionary.
func (d *Dictionary) Contains(word string) bool {
	_, ok := d.cities[word]
	return ok
}

// Pick returns a random unused city starting with first and with Diff <= maxDiff.
// With probability 0.7 it prefers cities from countries, if any qualify.
func (d *Dictionary) Pick(first rune, maxDiff uint8, countries []uint16, used func(string) bool, rnd *rand.Rand) (string, bool) {
	var all, preferred []string

	for _, name := range d.byRune[first] {
		c := d.cities[name]
		if c.Diff > maxDiff || used(name) {
			continue
		}

		all = append(all, name)

		for _, cc := range countries {
			if c.Country == cc {
				preferred = append(preferred, name)
				break
			}
		}
	}

	if len(all) == 0 {
		return "", false
	}

	pool := all
	if len(preferred) > 0 && rnd.Float64() < preferProb {
		pool = preferred
	}

	return pool[rnd.Intn(len(pool))], true
}

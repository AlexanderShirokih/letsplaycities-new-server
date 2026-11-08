// Package dict reads the .db2 city dictionary.
package dict

import (
	"bufio"
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

	cities map[string]City
	byRune map[rune][]string
}

// Parse reads a .db2 stream.
func Parse(r io.Reader) (*Dictionary, error) {
	br := bufio.NewReader(r)
	var hdr [12]byte
	if _, err := io.ReadFull(br, hdr[:]); err != nil {
		return nil, fmt.Errorf("dict: read header: %w", err)
	}
	if m := binary.BigEndian.Uint16(hdr[0:]); m != magic {
		return nil, fmt.Errorf("dict: invalid magic %#x", m)
	}
	if s := binary.BigEndian.Uint16(hdr[2:]); s != settingsV1 {
		return nil, fmt.Errorf("dict: unsupported settings %d", s)
	}
	version := binary.BigEndian.Uint32(hdr[4:])
	count := binary.BigEndian.Uint32(hdr[8:])

	d := &Dictionary{
		Version: int(version),
		cities:  make(map[string]City),
		byRune:  make(map[rune][]string),
	}
	for i := uint32(0); i <= count; i++ {
		var rh [4]byte
		if _, err := io.ReadFull(br, rh[:]); err != nil {
			return nil, fmt.Errorf("dict: record %d header: %w", i, err)
		}
		raw := make([]byte, rh[0])
		if _, err := io.ReadFull(br, raw); err != nil {
			return nil, fmt.Errorf("dict: record %d name: %w", i, err)
		}
		name := decodeName(raw)
		if i == count {
			if len(name) < checkSuffix || name[:len(name)-checkSuffix] != strconv.FormatUint(uint64(count), 10) {
				return nil, errors.New("dict: file check failed")
			}
			break
		}
		if name == "" {
			return nil, fmt.Errorf("dict: record %d has empty name", i)
		}
		c := City{Name: name, Diff: rh[1] & 3, Country: binary.BigEndian.Uint16(rh[2:])}
		if _, dup := d.cities[name]; !dup {
			first, _ := utf8.DecodeRuneInString(name)
			d.byRune[first] = append(d.byRune[first], name)
		}
		d.cities[name] = c
	}
	return d, nil
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
	entries, err := os.ReadDir(dir)
	if err != nil {
		return nil, "", err
	}
	best, bestN := "", -1
	for _, e := range entries {
		m := fileRe.FindStringSubmatch(e.Name())
		if m == nil || e.IsDir() {
			continue
		}
		n, err := strconv.Atoi(m[1])
		if err != nil {
			continue
		}
		if n > bestN {
			best, bestN = e.Name(), n
		}
	}
	if bestN < 0 {
		return nil, "", fmt.Errorf("dict: no data-N.db2 files in %s", dir)
	}
	path := filepath.Join(dir, best)
	f, err := os.Open(path)
	if err != nil {
		return nil, "", err
	}
	defer f.Close()
	d, err := Parse(f)
	if err != nil {
		return nil, "", fmt.Errorf("%s: %w", path, err)
	}
	return d, path, nil
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

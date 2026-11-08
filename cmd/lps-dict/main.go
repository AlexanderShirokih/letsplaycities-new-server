// Command lps-dict writes a new dictionary version without the listed cities.
//
//	lps-dict -in data-80.db2 -drop drop.tsv -version 81 -out data-81.db2
//
// The drop file holds one city per line: the name, optionally followed by tab-separated notes.
// Empty lines and lines starting with # are skipped.
// Every listed city must exist in -in.
//
// Exit codes: 0 written, 2 usage or data error.
package main

import (
	"bufio"
	"bytes"
	"flag"
	"fmt"
	"io"
	"os"
	"strings"

	"lps/internal/dict"
)

const (
	exitOK    = 0
	exitError = 2
)

func main() {
	os.Exit(run(os.Args[1:], os.Stdout, os.Stderr))
}

func run(args []string, stdout, stderr io.Writer) int {
	fs := flag.NewFlagSet("lps-dict", flag.ContinueOnError)
	fs.SetOutput(stderr)

	in := fs.String("in", "", "source dictionary (data-N.db2)")
	drop := fs.String("drop", "", "cities to remove, one per line")
	version := fs.Uint("version", 0, "version of the new dictionary, greater than the source")
	out := fs.String("out", "", "file to write (data-M.db2)")

	if err := fs.Parse(args); err != nil {
		return exitError
	}

	if *in == "" || *drop == "" || *version == 0 || *out == "" || fs.NArg() > 0 {
		_, _ = fmt.Fprintln(stderr, "usage: lps-dict -in data-N.db2 -drop drop.tsv -version M -out data-M.db2")

		return exitError
	}

	if err := rewrite(*in, *drop, uint32(*version), *out, stdout); err != nil {
		_, _ = fmt.Fprintln(stderr, "lps-dict:", err)

		return exitError
	}

	return exitOK
}

func rewrite(in, dropPath string, version uint32, out string, stdout io.Writer) error {
	d, err := load(in)
	if err != nil {
		return err
	}

	if int(version) <= d.Version {
		return fmt.Errorf("version %d must be greater than %d", version, d.Version)
	}

	drop, err := readDrop(dropPath)
	if err != nil {
		return err
	}

	var unknown []string

	for name := range drop {
		if !d.Contains(name) {
			unknown = append(unknown, name)
		}
	}

	if len(unknown) > 0 {
		return fmt.Errorf("not in the dictionary: %s", strings.Join(unknown, ", "))
	}

	var kept []dict.City

	for _, c := range d.Cities() {
		if !drop[c.Name] {
			kept = append(kept, c)
		}
	}

	var buf bytes.Buffer
	if err := dict.Write(&buf, version, kept); err != nil {
		return err
	}

	if err := os.WriteFile(out, buf.Bytes(), 0o644); err != nil {
		return err
	}

	_, _ = fmt.Fprintf(stdout, "%s: v%d, %d cities (%d dropped)\n", out, version, len(kept), len(drop))

	return nil
}

func load(path string) (*dict.Dictionary, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = f.Close() }()

	return dict.Parse(f)
}

func readDrop(path string) (map[string]bool, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = f.Close() }()

	drop := make(map[string]bool)
	sc := bufio.NewScanner(f)

	for sc.Scan() {
		name, _, _ := strings.Cut(sc.Text(), "\t")

		name = strings.TrimSpace(name)
		if name != "" && !strings.HasPrefix(name, "#") {
			drop[name] = true
		}
	}

	return drop, sc.Err()
}

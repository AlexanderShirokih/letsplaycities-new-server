package mysqldump

import (
	"bytes"
	"errors"
	"os"
	"strings"
	"testing"
)

func readMini(t *testing.T, tables ...string) map[string][]Row {
	t.Helper()

	f, err := os.Open("../../testdata/dump/mini.sql")
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() { _ = f.Close() })

	want := map[string]bool{}
	for _, n := range tables {
		want[n] = true
	}

	rows := map[string][]Row{}

	err = Read(f, want, func(r Row) error {
		rows[r.Table] = append(rows[r.Table], r)

		return nil
	})
	if err != nil {
		t.Fatal(err)
	}

	return rows
}

func TestReadRows(t *testing.T) {
	rows := readMini(t, "User", "Picture", "History")

	if len(rows["User"]) != 3 || len(rows["Picture"]) != 3 || len(rows["History"]) != 2 {
		t.Fatalf("counts: User=%d Picture=%d History=%d", len(rows["User"]), len(rows["Picture"]), len(rows["History"]))
	}

	if got := len(rows["User"][0].Values); got != 4 {
		t.Fatalf("User columns = %d", got)
	}

	if string(rows["User"][0].Values[2].Raw) != "-5" || string(rows["User"][0].Values[3].Raw) != "1.5" {
		t.Fatalf("numbers: %q %q", rows["User"][0].Values[2].Raw, rows["User"][0].Values[3].Raw)
	}

	if v := rows["User"][2].Values[1]; v.Null || len(v.Raw) != 0 {
		t.Fatalf("empty string = %+v", v)
	}
}

func TestReadBlobExact(t *testing.T) {
	rows := readMini(t, "Picture")

	want := []byte{0x00, 0x27, 0x5C, 0x0A, 0x1A, ')', ',', '(', 0xFF}
	if got := rows["Picture"][0].Values[1].Raw; !bytes.Equal(got, want) {
		t.Fatalf("blob = %v, want %v", got, want)
	}

	want = []byte("a;b\nINSERT INTO `User` VALUES (9,1);\n),(3,'z")
	if got := rows["Picture"][1].Values[1].Raw; !bytes.Equal(got, want) {
		t.Fatalf("blob2 = %q, want %q", got, want)
	}

	if rows["Picture"][2].Values[0].Raw[0] != '3' {
		t.Fatal("tuples after tricky blob lost")
	}
}

func TestReadNull(t *testing.T) {
	rows := readMini(t, "History")

	if !rows["History"][0].Values[1].Null || !rows["History"][1].Values[2].Null {
		t.Fatal("NULL not detected")
	}

	if v := rows["History"][0].Values[2]; v.Null || string(v.Raw) != "NULL" {
		t.Fatalf("quoted 'NULL' = %+v", v)
	}
}

func TestReadSkipsOtherTables(t *testing.T) {
	rows := readMini(t, "User")

	if len(rows) != 1 || len(rows["User"]) != 3 {
		t.Fatalf("rows = %v", rows)
	}
}

func TestReadUTF8(t *testing.T) {
	rows := readMini(t, "User")

	if got := string(rows["User"][0].Values[1].Raw); got != "Вася 'Пупкин'" {
		t.Fatalf("name = %q", got)
	}
}

func TestReadLargeLine(t *testing.T) {
	var sb strings.Builder

	sb.WriteString("INSERT INTO `Picture` VALUES (1,'")
	sb.WriteString(strings.Repeat("x\\'y", 1_500_000))
	sb.WriteString("'),(2,'z');\n")

	var got []Row

	err := Read(strings.NewReader(sb.String()), map[string]bool{"Picture": true}, func(r Row) error {
		got = append(got, r)

		return nil
	})
	if err != nil {
		t.Fatal(err)
	}

	if len(got) != 2 || len(got[0].Values[1].Raw) != 3*1_500_000 {
		t.Fatalf("rows=%d", len(got))
	}
}

func TestReadFnError(t *testing.T) {
	stop := errors.New("stop")
	n := 0

	err := Read(strings.NewReader("INSERT INTO `T` VALUES (1),(2),(3);\n"), map[string]bool{"T": true}, func(Row) error {
		n++

		return stop
	})
	if !errors.Is(err, stop) || n != 1 {
		t.Fatalf("err=%v n=%d", err, n)
	}
}

func TestReadMalformed(t *testing.T) {
	cases := map[string]string{
		"unterminated":   "INSERT INTO `T` VALUES (1,'abc);\n",
		"between tuples": "-- c\nINSERT INTO `T` VALUES (1)x(2);\n",
		"truncated":      "INSERT INTO `T` VALUES (1),(2",
	}

	for name, in := range cases {
		t.Run(name, func(t *testing.T) {
			err := Read(strings.NewReader(in), map[string]bool{"T": true}, func(Row) error { return nil })
			if err == nil || !strings.Contains(err.Error(), `"T"`) || !strings.Contains(err.Error(), "offset") {
				t.Fatalf("err = %v", err)
			}
		})
	}
}

func TestReadEscapes(t *testing.T) {
	in := "INSERT INTO `T` VALUES ('\\0\\'\\\"\\\\\\n\\r\\Z\\b\\t\\q');\n"
	want := []byte{0, '\'', '"', '\\', '\n', '\r', 0x1A, '\b', '\t', 'q'}

	var got []byte

	err := Read(strings.NewReader(in), map[string]bool{"T": true}, func(r Row) error {
		got = r.Values[0].Raw

		return nil
	})
	if err != nil || !bytes.Equal(got, want) {
		t.Fatalf("got %v err %v", got, err)
	}
}

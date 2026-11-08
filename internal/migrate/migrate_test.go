package migrate

import (
	"bytes"
	"compress/gzip"
	"context"
	"crypto/md5"
	"crypto/sha256"
	"database/sql"
	"encoding/base64"
	"encoding/hex"
	"fmt"
	"image"
	"image/color"
	"image/gif"
	"image/png"
	"os"
	"path/filepath"
	"reflect"
	"strconv"
	"strings"
	"testing"
	"time"

	"lps/internal/auth"
)

const testPepper = "test-pepper"

// sqlDump builds synthetic mysqldump text: extended INSERTs with mysqldump escaping.
type sqlDump struct{ strings.Builder }

func (d *sqlDump) insert(table string, rows ...[]any) {
	d.WriteString("INSERT INTO `" + table + "` VALUES ")

	for i, r := range rows {
		if i > 0 {
			d.WriteString(",")
		}

		vals := make([]string, len(r))
		for j, v := range r {
			vals[j] = sqlValue(v)
		}

		d.WriteString("(" + strings.Join(vals, ",") + ")")
	}

	d.WriteString(";\n")
}

func sqlValue(v any) string {
	switch v := v.(type) {
	case nil:
		return "NULL"
	case int:
		return strconv.Itoa(v)
	case string:
		return quote([]byte(v))
	case []byte:
		return quote(v)
	default:
		panic(fmt.Sprintf("unsupported %T", v))
	}
}

func quote(b []byte) string {
	esc := map[byte]string{0: `\0`, '\'': `\'`, '"': `\"`, '\\': `\\`, '\n': `\n`, '\r': `\r`, 0x1a: `\Z`}

	var s strings.Builder

	s.WriteByte('\'')

	for _, c := range b {
		if e, ok := esc[c]; ok {
			s.WriteString(e)

			continue
		}

		s.WriteByte(c)
	}

	s.WriteByte('\'')

	return s.String()
}

func writeDump(t *testing.T, d *sqlDump) string {
	t.Helper()

	path := filepath.Join(t.TempDir(), "backup.sql.gz")

	var buf bytes.Buffer

	zw := gzip.NewWriter(&buf)
	if _, err := zw.Write([]byte(d.String())); err != nil {
		t.Fatal(err)
	}

	if err := zw.Close(); err != nil {
		t.Fatal(err)
	}

	if err := os.WriteFile(path, buf.Bytes(), 0o600); err != nil {
		t.Fatal(err)
	}

	return path
}

func options(in, out string) Options {
	return Options{In: in, Out: out, Pepper: []byte(testPepper), BotIDs: []int64{12407}}
}

// migrate runs Run on d and opens the result for queries.
func migrate(t *testing.T, d *sqlDump) (Report, *sql.DB) {
	t.Helper()

	out := filepath.Join(t.TempDir(), "lps.db")

	rep, err := Run(context.Background(), options(writeDump(t, d), out))
	if err != nil {
		t.Fatal(err)
	}

	db, err := sql.Open("sqlite", out)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() { _ = db.Close() })

	return rep, db
}

func pngBytes(t *testing.T, c color.Color) []byte {
	t.Helper()

	img := image.NewRGBA(image.Rect(0, 0, 2, 2))
	img.Set(0, 0, c)

	var buf bytes.Buffer
	if err := png.Encode(&buf, img); err != nil {
		t.Fatal(err)
	}

	return buf.Bytes()
}

func gifBytes(t *testing.T) []byte {
	t.Helper()

	img := image.NewPaletted(image.Rect(0, 0, 2, 2), color.Palette{color.Black, color.White})

	var buf bytes.Buffer
	if err := gif.Encode(&buf, img, nil); err != nil {
		t.Fatal(err)
	}

	return buf.Bytes()
}

func md5Hex(b []byte) string {
	sum := md5.Sum(b)

	return hex.EncodeToString(sum[:])
}

func newHash(b []byte) string {
	sum := sha256.Sum256(b)

	return hex.EncodeToString(sum[:])[:16]
}

func user(id int, name string, reg, last, sn, avatarHash any, role any, authType int) []any {
	return []any{id, name, reg, last, sn, "acchash", "fcm-token", avatarHash, role, authType}
}

// baseDump is the synthetic dump shared by most tests; tables come in alphabetical order, User last.
func baseDump(t *testing.T) *sqlDump {
	t.Helper()

	png1 := pngBytes(t, color.White)

	var d sqlDump

	d.WriteString("-- synthetic dump\n/*!40103 SET TIME_ZONE='+00:00' */;\n")
	d.insert("Banlist", []any{1, 3}, []any{3, 999})
	d.insert("CityEditRequest",
		[]any{5, 1, 10, 20, "Москва", "Мск", "typo", nil, "APPROVED"},
		[]any{6, 2, 0, 0, nil, "Город", nil, nil, nil},
		[]any{7, 999, 1, 1, "a", "b", nil, nil, "NEW"})
	d.insert("ForbiddenLogin", []any{"admin"}, []any{"root"})
	d.insert("Friendship",
		[]any{1, 2, 1, "2020-02-03 04:05:06"},
		[]any{2, 3, nil, "2020-02-04 00:00:00"},
		[]any{1, 999, 0, "2020-02-05 00:00:00"},
		[]any{998, 1, 1, "2020-02-06 00:00:00"})
	d.insert("History",
		[]any{10, 1, 2, "2021-01-01 10:00:00", 120, 15},
		[]any{11, 2, 3, "2021-01-02 10:00:00", 0, 0},
		[]any{12, 1, 999, "2021-01-03 10:00:00", 50, 3},
		[]any{13, 3, 1, "2021-01-04 10:00:00", -5, 1},
		[]any{14, 2, 1, "2021-01-05 10:00:00", 30, 4})
	d.insert("Picture",
		[]any{1, png1, 1},
		[]any{2, base64.StdEncoding.EncodeToString(pngBytes(t, color.Black)), 0},
		[]any{3, gifBytes(t), 3},
		[]any{6, base64.StdEncoding.EncodeToString(pngBytes(t, color.Gray{Y: 128})), 1},
		[]any{999, png1, 1})
	d.insert("User",
		user(1, "Вася 'Пупкин'", "2019-05-01 12:34:56", "2020-01-02 03:04:05", "abcdef0123456789", md5Hex(png1), 1, 0),
		user(2, "Google", nil, "2021-06-07 08:09:10", "123456789012345678901", nil, 2, 1),
		user(3, "VK", "2018-01-01 00:00:00", nil, "777", nil, 0, 2),
		user(4, "VK twin", "2018-01-02 00:00:00", "2018-01-03 00:00:00", "777", "0123456789abcdef0123456789abcdef", nil, 2),
		user(5, "No sn", "2018-01-02 00:00:00", "2018-01-03 00:00:00", nil, nil, 1, 0),
		user(6, "Empty sn", "2018-01-02 00:00:00", "2018-01-03 00:00:00", "", nil, 1, 0),
		user(7, "Odd provider", "2018-01-02 00:00:00", "2018-01-03 00:00:00", "x", nil, 1, 9),
		user(12407, "Bot", "2018-01-02 00:00:00", "2018-01-03 00:00:00", "botdevice", nil, 1, 0))

	return &d
}

func tr(read, written int, dropped map[string]int) *TableReport {
	if dropped == nil {
		dropped = map[string]int{}
	}

	return &TableReport{Read: read, Written: written, Dropped: dropped}
}

func TestMigrateCounts(t *testing.T) {
	rep, _ := migrate(t, baseDump(t))

	want := map[string]*TableReport{
		"Banlist":         tr(2, 1, map[string]int{"missing_user": 1}),
		"CityEditRequest": tr(3, 2, map[string]int{"missing_user": 1}),
		"ForbiddenLogin":  tr(2, 2, nil),
		"Friendship":      tr(4, 2, map[string]int{"missing_user": 2}),
		"History":         tr(5, 2, map[string]int{"missing_user": 1, "bad_duration": 2}),
		"Picture":         tr(5, 4, map[string]int{"missing_user": 1}),
		"User":            tr(8, 8, nil),
		"identities": tr(8, 3, map[string]int{
			"bot": 1, "no_sn_uid": 2, "duplicate_identity": 1, "unknown_provider": 1,
		}),
	}

	if !reflect.DeepEqual(rep.Tables, want) {
		for name, w := range want {
			t.Errorf("%s: got %+v, want %+v", name, rep.Tables[name], w)
		}
	}

	if rep.AvatarChecked != 1 || rep.AvatarMD5Mismatch != 0 || rep.AvatarUndecodable != 0 || !rep.OK() {
		t.Fatalf("avatar report = %+v", rep)
	}
}

// ms is epoch ms of a UTC datetime (TIMESTAMP columns).
func ms(s string) int64 { return msIn(s, time.UTC) }

// mskMS is epoch ms of a Moscow datetime (User DATETIME columns).
func mskMS(s string) int64 { return msIn(s, time.FixedZone("MSK", 3*3600)) }

func msIn(s string, loc *time.Location) int64 {
	tm, err := time.ParseInLocation(time.DateTime, s, loc)
	if err != nil {
		panic(err)
	}

	return tm.UnixMilli()
}

func TestMigrateUsers(t *testing.T) {
	_, db := migrate(t, baseDump(t))

	type row struct {
		name          string
		created, seen int64
		role          int
		fcm           sql.NullString
	}

	want := map[int64]row{
		1: {"Вася 'Пупкин'", mskMS("2019-05-01 12:34:56"), mskMS("2020-01-02 03:04:05"), 1, sql.NullString{}},
		2: {"Google", mskMS("2021-06-07 08:09:10"), mskMS("2021-06-07 08:09:10"), 2, sql.NullString{}},
		3: {"VK", mskMS("2018-01-01 00:00:00"), mskMS("2018-01-01 00:00:00"), 0, sql.NullString{}},
		4: {"VK twin", mskMS("2018-01-02 00:00:00"), mskMS("2018-01-03 00:00:00"), 1, sql.NullString{}},
	}

	for id, w := range want {
		var got row

		err := db.QueryRow(`SELECT name, created_at, last_seen_at, role, fcm_token FROM users WHERE id = ?`, id).
			Scan(&got.name, &got.created, &got.seen, &got.role, &got.fcm)
		if err != nil {
			t.Fatal(err)
		}

		if got != w {
			t.Errorf("user %d = %+v, want %+v", id, got, w)
		}
	}
}

// TestMigrateTimeZones pins the zones: User DATETIME columns hold Moscow time,
// TIMESTAMP columns were dumped in UTC.
func TestMigrateTimeZones(t *testing.T) {
	var d sqlDump

	d.insert("Friendship", []any{1, 2, 1, "2020-01-01 03:00:00"})
	d.insert("History", []any{1, 1, 2, "2020-01-01 03:00:00", 10, 1})
	d.insert("User",
		user(1, "a", "2020-01-01 03:00:00", "2020-01-02 03:00:00", "s1", nil, 1, 1),
		user(2, "b", "2020-01-01 03:00:00", nil, "s2", nil, 1, 1))

	_, db := migrate(t, &d)

	var created, seen, friends, started int64

	err := db.QueryRow(`SELECT u.created_at, u.last_seen_at, f.created_at, g.started_at
		FROM users u, friendships f, games g WHERE u.id = 1`).Scan(&created, &seen, &friends, &started)
	if err != nil {
		t.Fatal(err)
	}

	at := func(day, hour int) int64 { return time.Date(2020, 1, day, hour, 0, 0, 0, time.UTC).UnixMilli() }

	if created != at(1, 0) || seen != at(2, 0) || friends != at(1, 3) || started != at(1, 3) {
		t.Fatalf("created %d, seen %d, friendship %d, game %d", created, seen, friends, started)
	}
}

func TestMigrateReferences(t *testing.T) {
	_, db := migrate(t, baseDump(t))

	queries := map[string]string{
		`SELECT group_concat(sender_id||'>'||receiver_id||':'||accepted||':'||created_at, ' ') FROM friendships`:                                                                                                         fmt.Sprintf("1>2:1:%d 2>3:0:%d", ms("2020-02-03 04:05:06"), ms("2020-02-04 00:00:00")),
		`SELECT group_concat(blocker_id||'>'||blocked_id, ' ') FROM blocks`:                                                                                                                                              "1>3",
		`SELECT group_concat(id||':'||starter_id||'>'||invited_id||':'||started_at||':'||duration_sec||':'||words, ' ') FROM games`:                                                                                      fmt.Sprintf("10:1>2:%d:120:15 14:2>1:%d:30:4", ms("2021-01-01 10:00:00"), ms("2021-01-05 10:00:00")),
		`SELECT group_concat(id||':'||user_id||':'||old_country||':'||new_country||':'||coalesce(old_name,'-')||':'||new_name||':'||coalesce(reason,'-')||':'||coalesce(verdict,'-')||':'||status, ' ') FROM city_edits`: "5:1:10:20:Москва:Мск:typo:-:APPROVED 6:2:0:0:-:Город:-:-:NEW",
		`SELECT group_concat(value, ' ') FROM forbidden_logins`:                                                                                                                                                          "admin root",
	}

	for q, want := range queries {
		var got string
		if err := db.QueryRow(q).Scan(&got); err != nil {
			t.Fatal(err)
		}

		if got != want {
			t.Errorf("%s\n got %q\nwant %q", q, got, want)
		}
	}
}

func TestMigrateNativeHashed(t *testing.T) {
	_, db := migrate(t, baseDump(t))

	got := map[string]string{}

	rows, err := db.Query(`SELECT provider, uid FROM identities ORDER BY user_id`)
	if err != nil {
		t.Fatal(err)
	}

	defer func() { _ = rows.Close() }()

	for rows.Next() {
		var p, uid string
		if err := rows.Scan(&p, &uid); err != nil {
			t.Fatal(err)
		}

		got[p] = uid
	}

	if err := rows.Err(); err != nil {
		t.Fatal(err)
	}

	want := map[string]string{
		"native": auth.NativeUID([]byte(testPepper), "abcdef0123456789"),
		"google": "123456789012345678901",
		"vk":     "777",
	}

	if !reflect.DeepEqual(got, want) {
		t.Fatalf("identities = %v, want %v", got, want)
	}
}

func TestMigrateBotsNoIdentity(t *testing.T) {
	_, db := migrate(t, baseDump(t))

	var users, idents int

	err := db.QueryRow(`SELECT (SELECT COUNT(*) FROM users WHERE id = 12407), (SELECT COUNT(*) FROM identities WHERE user_id = 12407)`).
		Scan(&users, &idents)
	if err != nil {
		t.Fatal(err)
	}

	if users != 1 || idents != 0 {
		t.Fatalf("bot: users=%d identities=%d", users, idents)
	}
}

func avatar(t *testing.T, db *sql.DB, id int64) (mime string, data []byte, hash sql.NullString) {
	t.Helper()

	err := db.QueryRow(`SELECT u.avatar_hash, COALESCE(a.mime, ''), a.data FROM users u LEFT JOIN avatars a ON a.user_id = u.id WHERE u.id = ?`, id).
		Scan(&hash, &mime, &data)
	if err != nil {
		t.Fatal(err)
	}

	return mime, data, hash
}

func TestMigrateAvatars(t *testing.T) {
	_, db := migrate(t, baseDump(t))

	wantData := map[int64][]byte{
		1: pngBytes(t, color.White),
		2: pngBytes(t, color.Black),
		3: gifBytes(t),
		6: pngBytes(t, color.Gray{Y: 128}), // BASE64 text under a non-zero type
	}
	wantMIME := map[int64]string{1: "image/png", 2: "image/png", 3: "image/gif", 6: "image/png"}

	for id, w := range wantData {
		mime, data, hash := avatar(t, db, id)
		if mime != wantMIME[id] || !bytes.Equal(data, w) || hash.String != newHash(w) || len(hash.String) != 16 {
			t.Errorf("user %d: mime %q, %d bytes, hash %q", id, mime, len(data), hash.String)
		}
	}

	if mime, _, hash := avatar(t, db, 4); mime != "" || hash.Valid {
		t.Errorf("user 4 without picture: mime %q, hash %v", mime, hash)
	}
}

func TestMigrateUndecodable(t *testing.T) {
	var d sqlDump

	d.insert("Picture", []any{1, "not an image at all", 2}, []any{2, "!!!not base64!!!", 0}, []any{3, "bm90IGFuIGltYWdl", 0})
	d.insert("User",
		user(1, "a", "2019-01-01 00:00:00", nil, "s1", nil, 1, 1),
		user(2, "b", "2019-01-01 00:00:00", nil, "s2", nil, 1, 1),
		user(3, "c", "2019-01-01 00:00:00", nil, "s3", nil, 1, 1))

	rep, db := migrate(t, &d)

	if got := rep.Tables["Picture"]; !reflect.DeepEqual(got, tr(3, 0, map[string]int{"undecodable": 3})) {
		t.Fatalf("Picture = %+v", got)
	}

	if rep.AvatarUndecodable != 3 || rep.OK() {
		t.Fatalf("report = %+v, OK = %v", rep, rep.OK())
	}

	var n int
	if err := db.QueryRow(`SELECT COUNT(*) FROM users WHERE avatar_hash IS NOT NULL`).Scan(&n); err != nil {
		t.Fatal(err)
	}

	if n != 0 {
		t.Fatalf("%d users keep avatar_hash", n)
	}
}

// avatarDump holds 30 users with raw PNG avatars; the first mismatched of them carry a wrong old hash.
func avatarDump(t *testing.T, mismatched int) *sqlDump {
	t.Helper()

	img := pngBytes(t, color.White)

	var (
		d     sqlDump
		pics  [][]any
		users [][]any
	)

	for i := 1; i <= 30; i++ {
		hash := strings.ToUpper(md5Hex(img))
		if i <= mismatched {
			hash = "0123456789abcdef0123456789abcdef"
		}

		pics = append(pics, []any{i, img, 1})
		users = append(users, user(i, "u", "2019-01-01 00:00:00", nil, "sn"+strconv.Itoa(i), hash, 1, 2))
	}

	d.insert("Picture", pics...)
	d.insert("User", users...)

	return &d
}

func TestMigrateMD5Mismatch(t *testing.T) {
	for _, tc := range []struct {
		mismatched int
		ok         bool
	}{{0, true}, {1, true}, {2, false}} {
		rep, _ := migrate(t, avatarDump(t, tc.mismatched))

		if rep.AvatarChecked != 30 || rep.AvatarMD5Mismatch != tc.mismatched || rep.OK() != tc.ok {
			t.Errorf("mismatched %d: checked %d, mismatch %d, OK %v", tc.mismatched, rep.AvatarChecked, rep.AvatarMD5Mismatch, rep.OK())
		}

		if rep.Tables["Picture"].Written != 30 {
			t.Errorf("mismatched %d: written %d", tc.mismatched, rep.Tables["Picture"].Written)
		}
	}
}

func TestMigrateRefusesExisting(t *testing.T) {
	in := writeDump(t, baseDump(t))
	out := filepath.Join(t.TempDir(), "lps.db")

	if err := os.WriteFile(out, []byte("old"), 0o600); err != nil {
		t.Fatal(err)
	}

	_, err := Run(context.Background(), options(in, out))
	if err == nil || !strings.Contains(err.Error(), "already exists") {
		t.Fatalf("err = %v", err)
	}

	if b, _ := os.ReadFile(out); string(b) != "old" {
		t.Fatal("existing file changed")
	}

	o := options(in, out)
	o.Force = true

	if _, err := Run(context.Background(), o); err != nil {
		t.Fatalf("force: %v", err)
	}
}

func TestMigrateMissingInput(t *testing.T) {
	dir := t.TempDir()
	out := filepath.Join(dir, "lps.db")

	_, err := Run(context.Background(), options(filepath.Join(dir, "absent.sql.gz"), out))
	if err == nil {
		t.Fatal("no error for a missing input")
	}

	if _, err := os.Stat(out); !os.IsNotExist(err) {
		t.Fatalf("output left behind: %v", err)
	}
}

func TestMigrateRealDump(t *testing.T) {
	in := os.Getenv("LPS_TEST_DUMP")
	if in == "" {
		t.Skip("LPS_TEST_DUMP not set: give the absolute path to backup.sql.gz")
	}

	if !filepath.IsAbs(in) {
		t.Fatal("LPS_TEST_DUMP must be an absolute path: go test runs in the package directory")
	}

	o := options(in, filepath.Join(t.TempDir(), "lps.db"))
	o.BotIDs = []int64{12407, 12408, 14397, 14398, 14399, 14400}

	rep, err := Run(context.Background(), o)
	if err != nil {
		t.Fatal(err)
	}

	t.Log("\n" + rep.String())

	if !rep.OK() || rep.Tables["User"].Written != rep.Tables["User"].Read {
		t.Fatal("report not OK")
	}
}

func TestMigrateWritesPepperCheck(t *testing.T) {
	_, db := migrate(t, baseDump(t))

	var check string

	if err := db.QueryRow(`SELECT pepper_check FROM meta WHERE id = 1`).Scan(&check); err != nil {
		t.Fatal(err)
	}

	if check != auth.PepperCheck([]byte(testPepper)) {
		t.Fatalf("pepper_check = %s", check)
	}
}

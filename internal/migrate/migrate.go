// Package migrate converts the old MariaDB dump (gzip) into the SQLite database.
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
	"errors"
	"fmt"
	"image"
	_ "image/gif"  // register GIF for image.DecodeConfig
	_ "image/jpeg" // register JPEG for image.DecodeConfig
	_ "image/png"  // register PNG for image.DecodeConfig
	"net/url"
	"os"
	"sort"
	"strconv"
	"strings"
	"text/tabwriter"
	"time"

	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/auth"
	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/mysqldump"
	"github.com/AlexanderShirokih/letsplaycities-new-server/internal/store"
)

// Options configures Run.
type Options struct {
	In, Out string
	Force   bool
	Pepper  []byte
	BotIDs  []int64
}

// TableReport counts the rows of one table. Read = Written + all Dropped.
type TableReport struct {
	Read, Written int
	Dropped       map[string]int // reason → count
}

// Report is the outcome of a migration. It holds counts only, never row contents.
type Report struct {
	Tables                           map[string]*TableReport
	AvatarMD5Mismatch, AvatarChecked int
	AvatarUndecodable                int
}

// identitiesTable is the report entry for the identities derived from User rows.
const identitiesTable = "identities"

// Run migrates the dump o.In into a new database at o.Out. The dump is read twice:
// User first (it is the last table of the dump), then the tables that reference it.
func Run(ctx context.Context, o Options) (Report, error) {
	if len(o.Pepper) == 0 {
		return Report{}, errors.New("empty native pepper")
	}

	if err := checkInput(o.In); err != nil {
		return Report{}, err
	}

	if err := prepareOut(o.Out, o.Force); err != nil {
		return Report{}, err
	}

	rep, err := load(ctx, o)
	if err != nil {
		_ = removeOut(o.Out)

		return Report{}, err
	}

	return rep, nil
}

// OK reports whether every avatar opened as an image and at most 5% of the checked md5 differ.
func (r Report) OK() bool {
	return r.AvatarUndecodable == 0 && r.AvatarMD5Mismatch*20 <= r.AvatarChecked
}

// String renders the report as a console table.
func (r Report) String() string {
	var b strings.Builder

	w := tabwriter.NewWriter(&b, 0, 0, 2, ' ', tabwriter.AlignRight)
	_, _ = fmt.Fprintln(w, "table\tread\twritten\tdropped\t")

	for _, name := range sortedKeys(r.Tables) {
		t := r.Tables[name]
		_, _ = fmt.Fprintf(w, "%s\t%d\t%d\t%d\t%s\n", name, t.Read, t.Written, t.Read-t.Written, reasons(t.Dropped))
	}

	_ = w.Flush()

	fmt.Fprintf(&b, "avatar md5: %d checked, %d mismatched (%.1f%%)\n",
		r.AvatarChecked, r.AvatarMD5Mismatch, percent(r.AvatarMD5Mismatch, r.AvatarChecked))
	fmt.Fprintf(&b, "avatars undecodable: %d\n", r.AvatarUndecodable)

	status := "OK"
	if !r.OK() {
		status = "NOT OK"
	}

	b.WriteString(status + "\n")

	return b.String()
}

func reasons(m map[string]int) string {
	parts := make([]string, 0, len(m))
	for _, k := range sortedKeys(m) {
		parts = append(parts, fmt.Sprintf("%s=%d", k, m[k]))
	}

	if len(parts) == 0 {
		return ""
	}

	return "  " + strings.Join(parts, " ")
}

func sortedKeys[V any](m map[string]V) []string {
	keys := make([]string, 0, len(m))
	for k := range m {
		keys = append(keys, k)
	}

	sort.Strings(keys)

	return keys
}

func percent(n, of int) float64 {
	if of == 0 {
		return 0
	}

	return 100 * float64(n) / float64(of)
}

func (r *Report) table(name string) *TableReport {
	t, ok := r.Tables[name]
	if !ok {
		t = &TableReport{Dropped: map[string]int{}}
		r.Tables[name] = t
	}

	return t
}

// record counts one read row: written when reason is empty, dropped for that reason otherwise.
func (t *TableReport) record(reason string) {
	t.Read++

	if reason == "" {
		t.Written++

		return
	}

	t.Dropped[reason]++
}

func checkInput(path string) error {
	f, zr, err := openDump(path)
	if err != nil {
		return err
	}

	_ = zr.Close()

	return f.Close()
}

func openDump(path string) (*os.File, *gzip.Reader, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, nil, fmt.Errorf("open dump: %w", err)
	}

	zr, err := gzip.NewReader(f)
	if err != nil {
		_ = f.Close()

		return nil, nil, fmt.Errorf("open dump: %w", err)
	}

	return f, zr, nil
}

func prepareOut(path string, force bool) error {
	_, err := os.Stat(path)
	if errors.Is(err, os.ErrNotExist) {
		return nil
	}

	if err != nil {
		return err
	}

	if !force {
		return fmt.Errorf("%s already exists (use -force to replace it)", path)
	}

	return removeOut(path)
}

// removeOut deletes the database file with its WAL companions.
func removeOut(path string) error {
	var errs []error

	for _, p := range []string{path, path + "-wal", path + "-shm"} {
		if err := os.Remove(p); err != nil && !errors.Is(err, os.ErrNotExist) {
			errs = append(errs, err)
		}
	}

	return errors.Join(errs...)
}

// load creates the schema with store.Open, then bulk-loads both passes in one transaction
// on a connection with foreign keys off; foreign_key_check must be clean before commit.
func load(ctx context.Context, o Options) (Report, error) {
	s, err := store.Open(o.Out)
	if err != nil {
		return Report{}, err
	}

	if err := s.Close(); err != nil {
		return Report{}, err
	}

	db, err := sql.Open("sqlite", "file:"+(&url.URL{Path: o.Out}).EscapedPath()+"?_pragma=foreign_keys(0)")
	if err != nil {
		return Report{}, err
	}

	defer func() { _ = db.Close() }()

	db.SetMaxOpenConns(1)

	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return Report{}, err
	}

	rep, err := loadTx(ctx, tx, o)
	if err != nil {
		_ = tx.Rollback()

		return Report{}, err
	}

	return rep, tx.Commit()
}

func loadTx(ctx context.Context, tx *sql.Tx, o Options) (Report, error) {
	m, err := newMigrator(ctx, tx, o)
	if err != nil {
		return Report{}, err
	}

	defer m.close()

	if err := m.pass(ctx, o.In, m.userPass()); err != nil {
		return Report{}, err
	}

	if err := m.pass(ctx, o.In, m.refPass()); err != nil {
		return Report{}, err
	}

	if err := checkForeignKeys(ctx, tx); err != nil {
		return Report{}, err
	}

	return m.rep, nil
}

func checkForeignKeys(ctx context.Context, tx *sql.Tx) error {
	rows, err := tx.QueryContext(ctx, `PRAGMA foreign_key_check`)
	if err != nil {
		return err
	}

	defer func() { _ = rows.Close() }()

	n := 0
	for rows.Next() {
		n++
	}

	if err := rows.Err(); err != nil {
		return err
	}

	if n > 0 {
		return fmt.Errorf("foreign_key_check: %d violations", n)
	}

	return nil
}

// loader moves one dump row into the database and returns a drop reason, or "" when written.
type loader struct {
	cols int
	fn   func(ctx context.Context, c *cols) (string, error)
}

func (m *migrator) pass(ctx context.Context, path string, loaders map[string]loader) error {
	f, zr, err := openDump(path)
	if err != nil {
		return err
	}

	defer func() { _ = f.Close() }()

	tables := map[string]bool{}
	for name := range loaders {
		tables[name] = true
		m.rep.table(name)
	}

	return mysqldump.Read(zr, tables, func(r mysqldump.Row) error {
		if err := ctx.Err(); err != nil {
			return err
		}

		return m.row(ctx, r, loaders[r.Table])
	})
}

// row runs the loader of r's table. Errors name the table and row number only, never values.
func (m *migrator) row(ctx context.Context, r mysqldump.Row, l loader) error {
	t := m.rep.table(r.Table)
	n := t.Read + 1

	if len(r.Values) != l.cols {
		return fmt.Errorf("%s row %d: %d columns, want %d", r.Table, n, len(r.Values), l.cols)
	}

	reason, err := l.fn(ctx, &cols{v: r.Values})
	if err != nil {
		return fmt.Errorf("%s row %d: %w", r.Table, n, err)
	}

	t.record(reason)

	return nil
}

// Prepared statements of the bulk load.
const (
	stUser       = "user"
	stIdentity   = "identity"
	stAvatar     = "avatar"
	stAvatarHash = "avatarHash"
	stFriendship = "friendship"
	stBlock      = "block"
	stGame       = "game"
	stCityEdit   = "cityEdit"
	stForbidden  = "forbidden"
)

var statements = map[string]string{
	stUser:       `INSERT INTO users(id, name, created_at, last_seen_at, role) VALUES(?, ?, ?, ?, ?)`,
	stIdentity:   `INSERT INTO identities(provider, uid, user_id) VALUES(?, ?, ?)`,
	stAvatar:     `INSERT INTO avatars(user_id, mime, data) VALUES(?, ?, ?)`,
	stAvatarHash: `UPDATE users SET avatar_hash = ? WHERE id = ?`,
	stFriendship: `INSERT INTO friendships(sender_id, receiver_id, accepted, created_at) VALUES(?, ?, ?, ?)`,
	stBlock:      `INSERT INTO blocks(blocker_id, blocked_id) VALUES(?, ?)`,
	stGame:       `INSERT INTO games(id, starter_id, invited_id, started_at, duration_sec, words) VALUES(?, ?, ?, ?, ?, ?)`,
	stCityEdit: `INSERT INTO city_edits(id, user_id, old_country, new_country, old_name, new_name, reason, verdict, status)
		VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)`,
	stForbidden: `INSERT INTO forbidden_logins(value) VALUES(?)`,
}

type migrator struct {
	stmts  map[string]*sql.Stmt
	pepper []byte
	bots   map[int64]bool
	users  map[int64]string // id → old avatar_hash, lower case ("" = none)
	idents map[string]bool  // provider + "\x00" + uid
	rep    Report
}

func newMigrator(ctx context.Context, tx *sql.Tx, o Options) (*migrator, error) {
	m := &migrator{
		stmts:  map[string]*sql.Stmt{},
		pepper: o.Pepper,
		bots:   map[int64]bool{},
		users:  map[int64]string{},
		idents: map[string]bool{},
		rep:    Report{Tables: map[string]*TableReport{}},
	}

	for _, id := range o.BotIDs {
		m.bots[id] = true
	}

	for name, q := range statements {
		st, err := tx.PrepareContext(ctx, q)
		if err != nil {
			m.close()

			return nil, err
		}

		m.stmts[name] = st
	}

	return m, nil
}

func (m *migrator) close() {
	for _, st := range m.stmts {
		_ = st.Close()
	}
}

func (m *migrator) exec(ctx context.Context, stmt string, args ...any) error {
	_, err := m.stmts[stmt].ExecContext(ctx, args...)

	return err
}

// known reports whether all ids belong to users loaded in the first pass.
func (m *migrator) known(ids ...int64) bool {
	for _, id := range ids {
		if _, ok := m.users[id]; !ok {
			return false
		}
	}

	return true
}

var errBadValue = errors.New("bad value")

// cols reads typed values out of one dump row. The first failure sticks in err;
// its text names the column index only, never the value.
type cols struct {
	v   []mysqldump.Value
	err error
}

func (c *cols) fail(i int) {
	if c.err == nil {
		c.err = fmt.Errorf("%w in column %d", errBadValue, i)
	}
}

func (c *cols) int(i int) int64 {
	if c.v[i].Null {
		c.fail(i)

		return 0
	}

	return c.intOr(i, 0)
}

func (c *cols) intOr(i int, def int64) int64 {
	if c.v[i].Null {
		return def
	}

	n, err := strconv.ParseInt(string(c.v[i].Raw), 10, 64)
	if err != nil {
		c.fail(i)
	}

	return n
}

func (c *cols) str(i int) string { return string(c.v[i].Raw) }

// text keeps NULL as nil for nullable TEXT columns.
func (c *cols) text(i int) any {
	if c.v[i].Null {
		return nil
	}

	return c.str(i)
}

// ms converts a UTC datetime to epoch ms; ok is false for NULL and the zero date.
func (c *cols) ms(i int) (int64, bool) {
	if c.v[i].Null || strings.HasPrefix(c.str(i), "0000-00-00") {
		return 0, false
	}

	t, err := time.Parse(time.DateTime, c.str(i))
	if err != nil {
		c.fail(i)

		return 0, false
	}

	return t.UnixMilli(), true
}

func (m *migrator) userPass() map[string]loader {
	return map[string]loader{"User": {cols: 10, fn: m.loadUser}}
}

func (m *migrator) refPass() map[string]loader {
	return map[string]loader{
		"Banlist":         {cols: 2, fn: m.loadBlock},
		"CityEditRequest": {cols: 9, fn: m.loadCityEdit},
		"ForbiddenLogin":  {cols: 1, fn: m.loadForbidden},
		"Friendship":      {cols: 4, fn: m.loadFriendship},
		"History":         {cols: 6, fn: m.loadGame},
		"Picture":         {cols: 3, fn: m.loadPicture},
	}
}

// providers maps the old auth_type to identities.provider.
var providers = []string{"native", "google", "vk", "ok", "fb"}

// loadUser: id, name, reg_date, last_visit, sn_uid, access_hash, firebase_token, avatar_hash, role, auth_type.
// access_hash and firebase_token are not carried over; avatar_hash is set by the Picture pass.
func (m *migrator) loadUser(ctx context.Context, c *cols) (string, error) {
	id, authType, role := c.int(0), c.int(9), c.intOr(8, 1)
	created, okCreated := c.ms(2)
	seen, okSeen := c.ms(3)

	if c.err != nil {
		return "", c.err
	}

	if !okCreated {
		created = seen
	}

	if !okSeen {
		seen = created
	}

	if err := m.exec(ctx, stUser, id, c.str(1), created, seen, role); err != nil {
		return "", err
	}

	m.users[id] = strings.ToLower(c.str(7))

	reason, err := m.addIdentity(ctx, id, authType, c.v[4])
	if err != nil {
		return "", err
	}

	m.rep.table(identitiesTable).record(reason)

	return "", nil
}

func (m *migrator) addIdentity(ctx context.Context, userID, authType int64, sn mysqldump.Value) (string, error) {
	switch {
	case m.bots[userID]:
		return "bot", nil
	case sn.Null || len(sn.Raw) == 0:
		return "no_sn_uid", nil
	case authType < 0 || authType >= int64(len(providers)):
		return "unknown_provider", nil
	}

	provider, uid := providers[authType], string(sn.Raw)
	if provider == "native" {
		uid = auth.NativeUID(m.pepper, uid)
	}

	key := provider + "\x00" + uid
	if m.idents[key] {
		return "duplicate_identity", nil
	}

	m.idents[key] = true

	return "", m.exec(ctx, stIdentity, provider, uid, userID)
}

// loadPicture: owner_id, image, type (0 BASE64, 1 PNG, 2 JPEG, 3 GIF).
func (m *migrator) loadPicture(ctx context.Context, c *cols) (string, error) {
	owner, typ := c.int(0), c.int(2)
	if c.err != nil {
		return "", c.err
	}

	oldHash, ok := m.users[owner]
	if !ok {
		return "missing_user", nil
	}

	data, mime, ok := m.avatarImage(c.v[1].Raw, typ, oldHash)
	if !ok {
		m.rep.AvatarUndecodable++

		return "undecodable", nil
	}

	if err := m.exec(ctx, stAvatar, owner, mime, data); err != nil {
		return "", err
	}

	sum := sha256.Sum256(data)

	return "", m.exec(ctx, stAvatarHash, hex.EncodeToString(sum[:])[:16], owner)
}

// avatarImage md5-checks a raw picture and sniffs its image type. Type 0 is BASE64,
// and some rows of other types hold BASE64 text too, so those are decoded as a fallback.
func (m *migrator) avatarImage(raw []byte, typ int64, oldHash string) ([]byte, string, bool) {
	if typ != 0 {
		m.checkMD5(raw, oldHash)

		if mime, ok := imageMIME(raw); ok {
			return raw, mime, true
		}
	}

	data, ok := decodeBase64(raw)
	if !ok {
		return nil, "", false
	}

	mime, ok := imageMIME(data)

	return data, mime, ok
}

// checkMD5 compares md5(image) with the old avatar_hash; users without a hash are not counted.
func (m *migrator) checkMD5(data []byte, oldHash string) {
	if oldHash == "" {
		return
	}

	m.rep.AvatarChecked++

	if sum := md5.Sum(data); hex.EncodeToString(sum[:]) != oldHash {
		m.rep.AvatarMD5Mismatch++
	}
}

// decodeBase64 accepts padded and unpadded standard base64; line breaks are ignored.
func decodeBase64(raw []byte) ([]byte, bool) {
	for _, enc := range []*base64.Encoding{base64.StdEncoding, base64.RawStdEncoding} {
		if b, err := enc.DecodeString(string(raw)); err == nil {
			return b, true
		}
	}

	return nil, false
}

var signatures = []struct{ prefix, mime string }{
	{"\x89PNG\r\n\x1a\n", "image/png"},
	{"\xff\xd8\xff", "image/jpeg"},
	{"GIF87a", "image/gif"},
	{"GIF89a", "image/gif"},
}

// imageMIME returns the MIME type by file signature if the data also opens as that image.
func imageMIME(data []byte) (string, bool) {
	for _, s := range signatures {
		if bytes.HasPrefix(data, []byte(s.prefix)) {
			_, _, err := image.DecodeConfig(bytes.NewReader(data))

			return s.mime, err == nil
		}
	}

	return "", false
}

// loadFriendship: sender_id, receiver_id, accepted, creation_date.
func (m *migrator) loadFriendship(ctx context.Context, c *cols) (string, error) {
	sender, receiver, accepted := c.int(0), c.int(1), c.intOr(2, 0)
	created, ok := c.ms(3)

	switch {
	case c.err != nil:
		return "", c.err
	case !m.known(sender, receiver):
		return "missing_user", nil
	case !ok:
		return "bad_time", nil
	}

	return "", m.exec(ctx, stFriendship, sender, receiver, accepted, created)
}

// loadBlock: baner_id, banned_id.
func (m *migrator) loadBlock(ctx context.Context, c *cols) (string, error) {
	blocker, blocked := c.int(0), c.int(1)

	switch {
	case c.err != nil:
		return "", c.err
	case !m.known(blocker, blocked):
		return "missing_user", nil
	}

	return "", m.exec(ctx, stBlock, blocker, blocked)
}

// loadGame: id, starter_id, invited_id, begin_time, duration, words_count.
func (m *migrator) loadGame(ctx context.Context, c *cols) (string, error) {
	id, starter, invited := c.int(0), c.int(1), c.int(2)
	duration, words := c.int(4), c.int(5)
	started, ok := c.ms(3)

	switch {
	case c.err != nil:
		return "", c.err
	case !m.known(starter, invited):
		return "missing_user", nil
	case duration <= 0:
		return "bad_duration", nil
	case !ok:
		return "bad_time", nil
	}

	return "", m.exec(ctx, stGame, id, starter, invited, started, duration, words)
}

// loadCityEdit: id, owner_id, old_country_code, new_country_code, old_name, new_name, reason, verdict, status.
func (m *migrator) loadCityEdit(ctx context.Context, c *cols) (string, error) {
	id, owner := c.int(0), c.int(1)
	oldCountry, newCountry := c.intOr(2, 0), c.intOr(3, 0)

	switch {
	case c.err != nil:
		return "", c.err
	case !m.known(owner):
		return "missing_user", nil
	}

	status := c.str(8)
	if c.v[8].Null {
		status = "NEW"
	}

	return "", m.exec(ctx, stCityEdit, id, owner, oldCountry, newCountry,
		c.text(4), c.text(5), c.text(6), c.text(7), status)
}

// loadForbidden: value.
func (m *migrator) loadForbidden(ctx context.Context, c *cols) (string, error) {
	return "", m.exec(ctx, stForbidden, c.str(0))
}

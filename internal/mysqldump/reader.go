// Package mysqldump streams INSERT rows out of a mysqldump file with extended inserts.
package mysqldump

import (
	"bufio"
	"bytes"
	"errors"
	"fmt"
	"io"
)

const (
	bufferSize   = 1 << 20
	insertPrefix = "INSERT INTO `"
	valuesKw     = "` VALUES "
	maxNameLen   = 128
)

// Value is a single column value: a string without quotes and with escapes expanded,
// or a bare token (number) as text. Null is set for an unquoted NULL.
type Value struct {
	Null bool
	Raw  []byte
}

// Row is one tuple of an INSERT statement. Values is owned by the caller.
type Row struct {
	Table  string
	Values []Value
}

// Read streams every tuple of INSERT statements for the tables in the set to fn.
// Other statements and tables are skipped. A non-nil error from fn stops Read and is returned.
func Read(r io.Reader, tables map[string]bool, fn func(Row) error) error {
	p := &parser{br: bufio.NewReaderSize(r, bufferSize), line: 1}

	for {
		table, ok, err := p.nextInsert(tables)
		if err != nil {
			return err
		}

		if !ok {
			return nil
		}

		p.table = table

		if err := p.statement(fn); err != nil {
			return err
		}
	}
}

type parser struct {
	br      *bufio.Reader
	table   string
	line    int
	off     int64
	scratch []byte
}

func (p *parser) errorf(format string, args ...any) error {
	return fmt.Errorf("mysqldump: table %q, line %d, offset %d: %s", p.table, p.line, p.off, fmt.Sprintf(format, args...))
}

func (p *parser) readByte() (byte, error) {
	b, err := p.br.ReadByte()
	if err != nil {
		if errors.Is(err, io.EOF) {
			return 0, p.errorf("unexpected end of input")
		}

		return 0, err
	}

	p.off++

	if b == '\n' {
		p.line++
	}

	return b, nil
}

func (p *parser) peekByte() (byte, bool, error) {
	b, err := p.br.Peek(1)
	if errors.Is(err, io.EOF) {
		return 0, false, nil
	}

	if err != nil {
		return 0, false, err
	}

	return b[0], true, nil
}

// skipLine discards the rest of the current line without buffering it.
func (p *parser) skipLine() (bool, error) {
	for {
		chunk, err := p.br.ReadSlice('\n')
		p.off += int64(len(chunk))

		switch {
		case err == nil:
			p.line++

			return true, nil
		case errors.Is(err, bufio.ErrBufferFull):
			continue
		case errors.Is(err, io.EOF):
			return false, nil
		default:
			return false, err
		}
	}
}

// nextInsert advances to the next INSERT statement of a wanted table and consumes its head.
func (p *parser) nextInsert(tables map[string]bool) (string, bool, error) {
	for {
		isInsert, eof, err := p.atInsert()
		if err != nil || eof {
			return "", false, err
		}

		if isInsert {
			table, wanted, err := p.insertHead(tables)
			if err != nil {
				return "", false, err
			}

			if wanted {
				return table, true, nil
			}
		}

		more, err := p.skipLine()
		if err != nil || !more {
			return "", false, err
		}
	}
}

// atInsert reports whether the line at the cursor starts with an INSERT INTO prefix.
func (p *parser) atInsert() (isInsert, eof bool, err error) {
	head, err := p.br.Peek(len(insertPrefix))
	if err != nil && !errors.Is(err, io.EOF) {
		return false, false, err
	}

	return string(head) == insertPrefix, len(head) == 0, nil
}

// insertHead consumes "INSERT INTO `T` VALUES " and reports whether T is wanted.
// When it is not, the rest of the line is left for the caller to skip.
func (p *parser) insertHead(tables map[string]bool) (string, bool, error) {
	if _, err := p.br.Discard(len(insertPrefix)); err != nil {
		return "", false, err
	}

	p.off += int64(len(insertPrefix))

	var name []byte

	for {
		b, err := p.readByte()
		if err != nil {
			return "", false, err
		}

		if b == '`' {
			break
		}

		if b == '\n' || len(name) >= maxNameLen {
			return "", false, p.errorf("malformed INSERT table name")
		}

		name = append(name, b)
	}

	table := string(name)
	p.table = table

	if !tables[table] {
		return table, false, nil
	}

	rest := valuesKw[1:]

	for i := 0; i < len(rest); i++ {
		b, err := p.readByte()
		if err != nil {
			return "", false, err
		}

		if b != rest[i] {
			return "", false, p.errorf("expected VALUES after table name")
		}
	}

	return table, true, nil
}

// statement parses "(…),(…);\n" and emits each tuple.
func (p *parser) statement(fn func(Row) error) error {
	for {
		values, err := p.tuple()
		if err != nil {
			return err
		}

		if err := fn(Row{Table: p.table, Values: values}); err != nil {
			return err
		}

		b, err := p.readByte()
		if err != nil {
			return err
		}

		switch b {
		case ',':
			continue
		case ';':
			return p.endStatement()
		default:
			return p.errorf("unexpected byte %q between tuples", b)
		}
	}
}

func (p *parser) endStatement() error {
	b, ok, err := p.peekByte()
	if err != nil || !ok {
		return err
	}

	if b == '\r' {
		_, _ = p.readByte()

		b, ok, err = p.peekByte()
		if err != nil || !ok {
			return err
		}
	}

	if b != '\n' {
		return p.errorf("unexpected byte %q after statement", b)
	}

	_, err = p.readByte()

	return err
}

func (p *parser) tuple() ([]Value, error) {
	b, err := p.readByte()
	if err != nil {
		return nil, err
	}

	if b != '(' {
		return nil, p.errorf("expected '(' at tuple start, got %q", b)
	}

	var values []Value

	for {
		v, err := p.value()
		if err != nil {
			return nil, err
		}

		values = append(values, v)

		b, err := p.readByte()
		if err != nil {
			return nil, err
		}

		switch b {
		case ',':
		case ')':
			return values, nil
		default:
			return nil, p.errorf("unexpected byte %q after value", b)
		}
	}
}

func (p *parser) value() (Value, error) {
	b, _, err := p.peekByte()
	if err != nil {
		return Value{}, err
	}

	if b == '\'' {
		_, _ = p.readByte()

		raw, err := p.stringBody()

		return Value{Raw: raw}, err
	}

	return p.bare()
}

func (p *parser) bare() (Value, error) {
	p.scratch = p.scratch[:0]

	for {
		b, ok, err := p.peekByte()
		if err != nil {
			return Value{}, err
		}

		if !ok || b == ',' || b == ')' {
			break
		}

		if b == '\n' || b == '(' || b == '\'' || b == ';' {
			return Value{}, p.errorf("unexpected byte %q in bare value", b)
		}

		_, _ = p.readByte()
		p.scratch = append(p.scratch, b)
	}

	if len(p.scratch) == 0 {
		return Value{}, p.errorf("empty value")
	}

	if string(p.scratch) == "NULL" {
		return Value{Null: true}, nil
	}

	return Value{Raw: bytes.Clone(p.scratch)}, nil
}

// stringBody reads up to the closing quote (the opening one is already consumed).
func (p *parser) stringBody() ([]byte, error) {
	p.scratch = p.scratch[:0]

	for {
		if _, err := p.br.Peek(1); err != nil {
			return nil, p.eofErr(err)
		}

		chunk, _ := p.br.Peek(p.br.Buffered())

		i := bytes.IndexAny(chunk, "\\'")
		if i < 0 {
			i = len(chunk)
		}

		p.scratch = append(p.scratch, chunk[:i]...)
		p.advance(chunk[:i])

		if i == len(chunk) {
			continue
		}

		done, err := p.special()
		if err != nil {
			return nil, err
		}

		if done {
			return bytes.Clone(p.scratch), nil
		}
	}
}

func (p *parser) eofErr(err error) error {
	if errors.Is(err, io.EOF) {
		return p.errorf("unterminated string")
	}

	return err
}

func (p *parser) advance(b []byte) {
	_, _ = p.br.Discard(len(b))
	p.off += int64(len(b))
	p.line += bytes.Count(b, []byte{'\n'})
}

// special handles a backslash or a quote; it reports true when the string ended.
func (p *parser) special() (bool, error) {
	b, err := p.readByte()
	if err != nil {
		return false, p.eofErr(err)
	}

	if b == '\'' {
		next, ok, err := p.peekByte()
		if err != nil {
			return false, err
		}

		if !ok || next != '\'' {
			return true, nil
		}

		_, _ = p.readByte()
		p.scratch = append(p.scratch, '\'')

		return false, nil
	}

	esc, err := p.readByte()
	if err != nil {
		return false, p.eofErr(err)
	}

	p.scratch = append(p.scratch, unescape(esc))

	return false, nil
}

func unescape(c byte) byte {
	switch c {
	case '0':
		return 0x00
	case 'n':
		return '\n'
	case 'r':
		return '\r'
	case 't':
		return '\t'
	case 'b':
		return '\b'
	case 'Z':
		return 0x1A
	default:
		return c
	}
}

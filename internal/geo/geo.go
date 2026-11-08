// Package geo reads and writes geo-N.bin: city coordinates in the record order of data-N.db2.
//
// Layout, big-endian:
//
//	u16 magic, u16 format, u32 dictVersion, u32 count,
//	count × {i24 lat, i24 lon}, u32 crc32 of all preceding bytes.
//
// Latitude is stored as round(deg·2²³/180), longitude as round(deg·2²³/360).
package geo

import (
	"encoding/binary"
	"errors"
	"fmt"
	"hash/crc32"
	"io"
	"math"
)

const (
	magic      = 0x6E0D
	format     = 1
	headerSize = 12
	pointSize  = 6
	scale      = 1 << 23
	maxI24     = 1<<23 - 1
)

// Point is a coordinate in degrees.
type Point struct {
	Lat, Lon float64
}

// Write encodes pts for dictionary version dictVersion.
func Write(w io.Writer, dictVersion uint32, pts []Point) error {
	buf := make([]byte, headerSize, headerSize+pointSize*len(pts)+4)
	binary.BigEndian.PutUint16(buf[0:], magic)
	binary.BigEndian.PutUint16(buf[2:], format)
	binary.BigEndian.PutUint32(buf[4:], dictVersion)
	binary.BigEndian.PutUint32(buf[8:], uint32(len(pts)))

	for i, p := range pts {
		if !(p.Lat >= -90 && p.Lat <= 90 && p.Lon >= -180 && p.Lon <= 180) {
			return fmt.Errorf("geo: point %d out of range: %+v", i, p)
		}

		buf = putI24(buf, encode(p.Lat, 180))
		buf = putI24(buf, encode(p.Lon, 360))
	}

	buf = binary.BigEndian.AppendUint32(buf, crc32.ChecksumIEEE(buf))

	_, err := w.Write(buf)

	return err
}

// Read decodes a geo-N.bin stream.
func Read(r io.Reader) (dictVersion uint32, pts []Point, err error) {
	data, err := io.ReadAll(r)
	if err != nil {
		return 0, nil, err
	}

	if len(data) < headerSize+4 {
		return 0, nil, errors.New("geo: file too short")
	}

	if m := binary.BigEndian.Uint16(data[0:]); m != magic {
		return 0, nil, fmt.Errorf("geo: invalid magic %#x", m)
	}

	if f := binary.BigEndian.Uint16(data[2:]); f != format {
		return 0, nil, fmt.Errorf("geo: unsupported format %d", f)
	}

	count := int(binary.BigEndian.Uint32(data[8:]))
	if len(data) != headerSize+pointSize*count+4 {
		return 0, nil, fmt.Errorf("geo: size %d does not match count %d", len(data), count)
	}

	body, sum := data[:len(data)-4], binary.BigEndian.Uint32(data[len(data)-4:])
	if crc32.ChecksumIEEE(body) != sum {
		return 0, nil, errors.New("geo: checksum mismatch")
	}

	pts = make([]Point, count)

	for i := range pts {
		rec := body[headerSize+pointSize*i:]
		pts[i] = Point{Lat: decode(getI24(rec[0:]), 180), Lon: decode(getI24(rec[3:]), 360)}
	}

	return binary.BigEndian.Uint32(data[4:]), pts, nil
}

func encode(deg, span float64) int32 {
	v := math.Round(deg * scale / span)

	return int32(math.Max(-maxI24, math.Min(maxI24, v)))
}

func decode(v int32, span float64) float64 {
	return float64(v) * span / scale
}

func putI24(b []byte, v int32) []byte {
	u := uint32(v) & 0xFFFFFF

	return append(b, byte(u>>16), byte(u>>8), byte(u))
}

func getI24(b []byte) int32 {
	u := uint32(b[0])<<16 | uint32(b[1])<<8 | uint32(b[2])

	return int32(u<<8) >> 8
}

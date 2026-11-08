// Package game contains the rules of the "Сыграем в Города" game.
package game

import "strings"

// Normalize returns the canonical form of a city name: surrounding spaces
// are trimmed, the name is lowercased, "ё" is replaced by "е" and runs of
// consecutive '-' are collapsed into one.
func Normalize(s string) string {
	s = strings.ToLower(strings.TrimSpace(s))
	s = strings.ReplaceAll(s, "ё", "е")

	var b strings.Builder
	b.Grow(len(s))
	var prev rune
	for _, r := range s {
		if !(r == '-' && prev == '-') {
			b.WriteRune(r)
		}
		prev = r
	}
	return b.String()
}

// LastSuitableRune returns the last rune of word that is not one of the
// letters "ь", "ъ", "ы", "ё". If there is no such rune, the first rune of
// word is returned. For an empty word it returns 0.
func LastSuitableRune(word string) rune {
	var first, last rune
	found := false
	for i, r := range word {
		if i == 0 {
			first = r
		}
		if !isBadEnding(r) {
			last = r
			found = true
		}
	}
	if !found {
		return first
	}
	return last
}

func isBadEnding(r rune) bool {
	switch r {
	case 'ь', 'ъ', 'ы', 'ё':
		return true
	}
	return false
}

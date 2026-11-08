package game

import "testing"

func TestNormalize(t *testing.T) {
	tests := []struct {
		in   string
		want string
	}{
		{"  Йошкар--Ола ", "йошкар-ола"},
		{"Орёл", "орел"},
		{"МОСКВА", "москва"},
		{"а---б", "а-б"},
	}
	for _, tt := range tests {
		if got := Normalize(tt.in); got != tt.want {
			t.Errorf("Normalize(%q) = %q, want %q", tt.in, got, tt.want)
		}
	}
}

func TestLastSuitableRune(t *testing.T) {
	tests := []struct {
		in   string
		want rune
	}{
		{"тверь", 'р'},
		{"грозный", 'й'},
		{"кызыл", 'л'},
		{"ы", 'ы'},
		{"", 0},
	}
	for _, tt := range tests {
		if got := LastSuitableRune(tt.in); got != tt.want {
			t.Errorf("LastSuitableRune(%q) = %q, want %q", tt.in, got, tt.want)
		}
	}
}

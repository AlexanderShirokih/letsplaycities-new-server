package auth

import (
	"sync"
	"time"
)

// Limiter is a fixed-window per-key rate limiter, safe for concurrent use.
type Limiter struct {
	limit  int
	window time.Duration
	now    func() time.Time

	mu   sync.Mutex
	hits map[string]*bucket
}

type bucket struct {
	start time.Time
	n     int
}

func NewLimiter(limit int, window time.Duration, now func() time.Time) *Limiter {
	return &Limiter{limit: limit, window: window, now: now, hits: map[string]*bucket{}}
}

// Allow reports whether key may proceed. Expired keys are dropped on access.
func (l *Limiter) Allow(key string) bool {
	l.mu.Lock()
	defer l.mu.Unlock()
	t := l.now()
	for k, b := range l.hits {
		if t.Sub(b.start) >= l.window {
			delete(l.hits, k)
		}
	}
	b := l.hits[key]
	if b == nil {
		b = &bucket{start: t}
		l.hits[key] = b
	}
	if b.n >= l.limit {
		return false
	}
	b.n++
	return true
}

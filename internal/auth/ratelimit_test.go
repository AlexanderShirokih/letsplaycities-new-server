package auth

import (
	"sync"
	"testing"
	"time"
)

func TestLimiter(t *testing.T) {
	now := time.Unix(1000, 0)

	l := NewLimiter(10, time.Minute, func() time.Time { return now })
	for i := 0; i < 10; i++ {
		if !l.Allow("ip") {
			t.Fatalf("call %d denied", i)
		}
	}

	if l.Allow("ip") {
		t.Fatal("11th allowed")
	}

	if !l.Allow("other") {
		t.Fatal("other key affected")
	}

	now = now.Add(61 * time.Second)

	if !l.Allow("ip") {
		t.Fatal("not reset after window")
	}
}

func TestLimiterDropsExpiredKeys(t *testing.T) {
	now := time.Unix(1000, 0)

	l := NewLimiter(1, time.Minute, func() time.Time { return now })
	for i := 0; i < 100; i++ {
		l.Allow(string(rune('a' + i)))
	}

	now = now.Add(2 * time.Minute)

	l.Allow("fresh")
	l.mu.Lock()
	n := len(l.hits)
	l.mu.Unlock()

	if n != 1 {
		t.Fatalf("keys retained: %d", n)
	}
}

func TestLimiterConcurrent(t *testing.T) {
	l := NewLimiter(50, time.Minute, time.Now)

	var (
		wg sync.WaitGroup
		mu sync.Mutex
	)

	ok := 0

	for i := 0; i < 200; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()

			if l.Allow("k") {
				mu.Lock()
				ok++
				mu.Unlock()
			}
		}()
	}

	wg.Wait()

	if ok != 50 {
		t.Fatalf("allowed %d", ok)
	}
}

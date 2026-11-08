package game

import (
	"sort"
	"sync"
	"time"
)

// Clock abstracts time so that rooms can be tested without sleeping.
type Clock interface {
	Now() time.Time
	// AfterFunc calls f in its own goroutine after d. The returned stop
	// function reports whether it prevented f from running.
	AfterFunc(d time.Duration, f func()) (stop func() bool)
}

// RealClock is the Clock backed by the time package.
type RealClock struct{}

// Now returns the current time.
func (RealClock) Now() time.Time { return time.Now() }

// AfterFunc schedules f with time.AfterFunc.
func (RealClock) AfterFunc(d time.Duration, f func()) func() bool {
	return time.AfterFunc(d, f).Stop
}

// FakeClock is a manually advanced Clock for tests.
type FakeClock struct {
	mu     sync.Mutex
	now    time.Time
	seq    int
	timers []*fakeTimer
}

type fakeTimer struct {
	at  time.Time
	seq int
	f   func()
}

// NewFakeClock returns a FakeClock that shows start.
func NewFakeClock(start time.Time) *FakeClock {
	return &FakeClock{now: start}
}

// Now returns the fake time.
func (c *FakeClock) Now() time.Time {
	c.mu.Lock()
	defer c.mu.Unlock()

	return c.now
}

// AfterFunc registers f to run when the fake time reaches now+d.
func (c *FakeClock) AfterFunc(d time.Duration, f func()) func() bool {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.seq++
	t := &fakeTimer{at: c.now.Add(d), seq: c.seq, f: f}
	c.timers = append(c.timers, t)

	return func() bool {
		c.mu.Lock()
		defer c.mu.Unlock()

		for i, x := range c.timers {
			if x == t {
				c.timers = append(c.timers[:i], c.timers[i+1:]...)

				return true
			}
		}

		return false
	}
}

// Advance moves the clock forward by d and synchronously runs every timer
// that became due, in time order. While a function runs, Now shows the
// moment it was due.
func (c *FakeClock) Advance(d time.Duration) {
	c.mu.Lock()
	target := c.now.Add(d)
	c.mu.Unlock()

	for {
		t := c.popDue(target)
		if t == nil {
			break
		}

		t.f()
	}

	c.mu.Lock()
	c.now = target
	c.mu.Unlock()
}

func (c *FakeClock) popDue(target time.Time) *fakeTimer {
	c.mu.Lock()
	defer c.mu.Unlock()

	sort.Slice(c.timers, func(i, j int) bool {
		a, b := c.timers[i], c.timers[j]
		if a.at.Equal(b.at) {
			return a.seq < b.seq
		}

		return a.at.Before(b.at)
	})

	if len(c.timers) == 0 || c.timers[0].at.After(target) {
		return nil
	}

	t := c.timers[0]
	c.timers = c.timers[1:]

	if t.at.After(c.now) {
		c.now = t.at
	}

	return t
}

package game

// step processes one queued event and reports whether there was one.
func (h *Hub) step() bool {
	select {
	case e := <-h.events:
		e()

		return true
	default:
		return false
	}
}

// flush processes events until the queue is empty.
func (h *Hub) flush() {
	for h.step() {
	}
}

// drain processes events until the queue is empty and no push is in
// flight.
func (h *Hub) drain() {
	for {
		h.flush()
		h.pushes.Wait()

		if len(h.events) == 0 {
			return
		}
	}
}

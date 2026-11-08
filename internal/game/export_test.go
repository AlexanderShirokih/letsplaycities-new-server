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

// drain processes events until the queue is empty.
func (h *Hub) drain() {
	for h.step() {
	}
}

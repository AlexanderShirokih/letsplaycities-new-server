package ru.quandastudio.lpsserver.core;

public class DelayedTask {
	private long nextTime;
	private int period;
	private boolean cancelled;
	private boolean periodic;
	private RunnableTask task;

	public DelayedTask(int delay, int period, RunnableTask task) {
		this.task = task;
		this.period = period;
		this.periodic = period != 0;
		this.nextTime = System.currentTimeMillis() + delay;
	}

	public boolean updateTask() {
		long curr = System.currentTimeMillis();
		if (nextTime < curr) {
			if (cancelled) {
				return true;
			}
			task.run(this);
			if (periodic) {
				nextTime = curr + period;
			} else {
				return true;
			}
		}
		return false;
	}

	public void cancel() {
		cancelled = true;
	}
}

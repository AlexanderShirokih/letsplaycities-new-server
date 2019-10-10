package ru.quandastudio.lpsserver.core;

import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

@Slf4j	
public class TaskLooper implements Runnable {
	private TaskLooper() {

	}

	private ArrayList<DelayedTask> tasks = new ArrayList<DelayedTask>(128);

	@Override
	public void run() {
		while (true) {
			synchronized (tasks) {
				while (tasks.isEmpty()) {
					try {
						tasks.wait();
					} catch (InterruptedException e) {

					}
				}

				tasks.removeIf((x) -> x.updateTask());
				try {
					tasks.wait(100);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public void schedule(DelayedTask task) {
		// This can block main thread while tasks iterates
		synchronized (tasks) {
			tasks.add(task);
			tasks.notify();
		}
	}

	public void shedule(int delay, int period, RunnableTask task) {
		schedule(new DelayedTask(delay, period, task));
	}

	public void logstate() {
		log.info(" TASK INFO SERVICE. task count={}", tasks.size());
	}

	private static final TaskLooper taskLooperInstance = new TaskLooper();

	public static TaskLooper getInstance() {
		return taskLooperInstance;
	}
}

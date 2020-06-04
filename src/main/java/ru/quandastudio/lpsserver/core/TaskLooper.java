package ru.quandastudio.lpsserver.core;

import java.util.ArrayList;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class TaskLooper extends Thread implements Runnable {

	private final ArrayList<DelayedTask> tasks = new ArrayList<>(128);

	@Override
	public void run() {
		while (true) {
			synchronized (tasks) {
				while (tasks.isEmpty()) {
					try {
						tasks.wait();
					} catch (InterruptedException e) {
						break;
					}
				}

				tasks.removeIf(DelayedTask::updateTask);
				try {
					tasks.wait(100);
				} catch (InterruptedException ignored) {
					break;
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

	public void log() {
		log.info(" TASK INFO SERVICE. task count={}", tasks.size());
	}

	public void shutdown() {
		interrupt();
	}
}

package ru.quandastudio.lpsserver;

import java.time.Duration;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.core.TaskLooper;
import ru.quandastudio.lpsserver.util.StringUtil;
import ru.quandastudio.lpsserver.websocket.SocketHandler;

@Slf4j
@RequiredArgsConstructor
@Component
public class LPSServer {

	private static long SERVER_UPTIME = 0;
	private static long lastTime;

	private final ServerContext context;

	public void start() {
		context.getBotManager().init();
		context.getTaskLooper().start();

		scheduleSystemTasks();
	}

	private void scheduleSystemTasks() {
		lastTime = System.currentTimeMillis();
		TaskLooper looper = context.getTaskLooper();

		looper.shedule(5000, 120000, (task) -> {
			SocketHandler.logstate();
			looper.log();
			context.log();
			context.getBotManager().log();
			log();
		});
	}

	@PreDestroy
	public void stop() {
		log.info("Shutting down LPS server...");
		context.getBotManager().shutdown();
		context.getTaskLooper().shutdown();
	}

	private static void log() {
		SERVER_UPTIME += System.currentTimeMillis() - lastTime;
		lastTime = System.currentTimeMillis();
		log.info("UPTIME: {}", StringUtil.formatDuration(Duration.ofMillis(SERVER_UPTIME)));
	}

}

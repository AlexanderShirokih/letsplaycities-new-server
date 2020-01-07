package ru.quandastudio.lpsserver.netty;

import java.net.InetSocketAddress;
import java.time.Duration;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.core.TaskLooper;
import ru.quandastudio.lpsserver.netty.handlers.ServerHandler;
import ru.quandastudio.lpsserver.util.StringUtil;
import ru.quandastudio.lpsserver.websocket.SocketHandler;
import ru.quandastudio.lpsserver.websocket.WebSocketMessageChannel;

@Slf4j
@RequiredArgsConstructor
@Component
public class LPSServer {

	private static long SERVER_UPTIME = 0;
	private static long lastTime;

	private final ServerBootstrap lpsBootstrap;

	private final InetSocketAddress tcpPort;

	private final ServerContext context;

	private Channel lpsChannel;

	public void start() {
		context.getBotManager().init();
		context.getTaskLooper().start();

		try {

			lpsChannel = lpsBootstrap.bind(tcpPort).sync().channel();
			log.info("Server is started : port {}", tcpPort.getPort());

			scheduleSystemTasks();

		} catch (InterruptedException e) {
			log.error("Error starting LPSServer. ", e);
		}
	}

	private void scheduleSystemTasks() {
		lastTime = System.currentTimeMillis();
		TaskLooper looper = context.getTaskLooper();

		looper.shedule(5000, 120000, (task) -> {
			ServerHandler.logstate();
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
		if (lpsChannel != null) {
			lpsChannel.close();
			lpsChannel.parent().close();
		}
		context.getBotManager().shutdown();
		context.getTaskLooper().shutdown();
	}

	private static void log() {
		SERVER_UPTIME += System.currentTimeMillis() - lastTime;
		lastTime = System.currentTimeMillis();
		log.info("UPTIME: {}", StringUtil.formatDuration(Duration.ofMillis(SERVER_UPTIME)));
	}

}

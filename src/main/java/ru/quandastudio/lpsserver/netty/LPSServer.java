package ru.quandastudio.lpsserver.netty;

import java.net.InetSocketAddress;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
@Component
public class LPSServer {

	private final ServerBootstrap lpsBootstrap;

	private final InetSocketAddress tcpPort;

	private Channel lpsChannel;

	public void start() {
		try {
			lpsChannel = lpsBootstrap.bind(tcpPort).sync().channel();
			log.info("Server is started : port {}", tcpPort.getPort());
			lpsChannel.closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@PreDestroy
	public void stop() {
		if (lpsChannel != null) {
			lpsChannel.close();
			lpsChannel.parent().close();
		}
	}
}

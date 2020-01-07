package ru.quandastudio.lpsserver.config;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.netty.LPSServerChannelInitializer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(NettyProperties.class)
public class NettyConfiguration {

	private final NettyProperties nettyProperties;

	@Bean
	public ServerBootstrap lpsBootstrap() {
		return new ServerBootstrap().group(bossGroup(), workerGroup())
				.channel(NioServerSocketChannel.class)
				.handler(new LoggingHandler())
				.childHandler(channelInitializer);
	}

	@Autowired
	private LPSServerChannelInitializer channelInitializer;

	@Bean(destroyMethod = "shutdownGracefully")
	public NioEventLoopGroup bossGroup() {
		return new NioEventLoopGroup(nettyProperties.getBossCount());
	}

	@Bean(destroyMethod = "shutdownGracefully")
	public NioEventLoopGroup workerGroup() {
		return new NioEventLoopGroup(nettyProperties.getWorkerCount());
	}

	@Bean
	public InetSocketAddress tcpSocketAddress() {
		return new InetSocketAddress(nettyProperties.getLpsPort());
	}

}

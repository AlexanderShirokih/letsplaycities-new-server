package ru.quandastudio.lpsserver.netty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.netty.handlers.ProtocolDetector;
import ru.quandastudio.lpsserver.netty.handlers.ServerHandler;

@Component
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class LPSServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final Gson gson;
	private final ServerContext serverContext;

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new ProtocolDetector(gson, new ServerHandler(serverContext)));
	}
}
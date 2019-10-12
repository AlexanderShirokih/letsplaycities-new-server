package ru.quandastudio.lpsserver.netty.handlers;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.netty.NettyMessageChannel;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage;

@Slf4j
@RequiredArgsConstructor
public class ServerHandler extends ChannelInboundHandlerAdapter {

	private final ServerContext serverContext;

	private static ConcurrentHashMap<Channel, Player> players = new ConcurrentHashMap<Channel, Player>();

	public static void logstate() {
		log.info("ONLINE: {}", players.size());
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		Channel channel = ctx.channel();

		Player p = Optional.ofNullable(players.get(channel)).or(() -> {
			final Player newPlayer = new Player(serverContext, new NettyMessageChannel(channel));
			players.put(channel, newPlayer);
			return Optional.of(newPlayer);
		}).get();

		p.onMessage((LPSClientMessage) msg);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		remove(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (cause instanceof IOException)
			log.warn("IOException cathed! MSG={}", cause.getMessage());
		else
			log.error("Caught unknown exception: ", cause);
		remove(ctx);
		ctx.close();
	}

	private void remove(ChannelHandlerContext ctx) {
		Player p = players.remove(ctx.channel());
		if (p != null)
			p.onDisconnected();
	}
}

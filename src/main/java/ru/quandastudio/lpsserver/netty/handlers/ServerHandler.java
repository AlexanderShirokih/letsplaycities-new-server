package ru.quandastudio.lpsserver.netty.handlers;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.actions.LoginAction;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.data.UserManager;
import ru.quandastudio.lpsserver.netty.NettyMessageChannel;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSLogIn;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ServerHandler extends ChannelInboundHandlerAdapter {

	private final UserManager userManager;

	private static ConcurrentHashMap<Channel, Player> players = new ConcurrentHashMap<Channel, Player>();

	public static void logstate() {
		log.info("ONLINE: {}", players.size());
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		final LPSClientMessage mwp = (LPSClientMessage) msg;

		Channel channel = ctx.channel();
		Player p = players.get(channel);
		if (p == null) {
			if (msg instanceof LPSClientMessage.LPSLogIn)
				processLoginAction(channel, (LPSLogIn) mwp);
			else
				log.warn("Waiting for LogIn message, bug received {}", mwp.getAction());

		} else {
			p.onMessage(mwp);
		}
	}

	private void processLoginAction(Channel channel, LPSLogIn msg) {
		LoginAction loginAction = new LoginAction(new NettyMessageChannel(channel), userManager);

		loginAction.logIn(msg).ifPresent((p) -> players.put(channel, p));
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

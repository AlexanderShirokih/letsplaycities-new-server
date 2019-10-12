package ru.quandastudio.lps.server.http;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import ru.quandastudio.lps.server.next.handlers.ServerHandler;

public class WebSocketHandler extends ChannelInboundHandlerAdapter {
	private static final Logger LOG = Logger.getLogger(WebSocketHandler.class.getSimpleName());

	private ServerHandler handler;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof TextWebSocketFrame) {
			if (handler == null) {
				handler = new ServerHandler();
			}

			handler.channelRead(ctx, new JSONObject(((TextWebSocketFrame) msg).text()));
			// ctx.channel()
			// .writeAndFlush(new TextWebSocketFrame("Message recieved : " +
			// ((TextWebSocketFrame) msg).text()));
		} else if (msg instanceof CloseWebSocketFrame) {
			// TODO
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (handler != null) {
			handler.channelInactive(ctx);
		} else
			super.channelInactive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (handler != null) {
			handler.exceptionCaught(ctx, cause);
		} else {
			LOG.log(Level.SEVERE, "EXCEPTION when handler == null! : ", cause);
		}
	}
}

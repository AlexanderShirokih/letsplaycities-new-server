package ru.quandastudio.lpsserver.netty;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import ru.quandastudio.lpsserver.core.MessageChannel;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;

@AllArgsConstructor
public class NettyMessageChannel implements MessageChannel {

	private final Channel channel;

	@Override
	public void send(LPSMessage message) {
		if (channel.isWritable()) {
			channel.writeAndFlush(message);
		}
	}

}

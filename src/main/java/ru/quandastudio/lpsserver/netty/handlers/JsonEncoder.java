package ru.quandastudio.lpsserver.netty.handlers;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;

@RequiredArgsConstructor
public class JsonEncoder extends MessageToByteEncoder<LPSMessage> {

	private final Gson gson;

	@Override
	protected void encode(ChannelHandlerContext ctx, LPSMessage msg, ByteBuf out) throws Exception {
		String json = gson.toJson(msg);

		out.writeCharSequence("size:", CharsetUtil.UTF_8);
		out.writeCharSequence(String.valueOf(json.length()), CharsetUtil.UTF_8);
		out.writeByte('\n');
		out.writeCharSequence(json, CharsetUtil.UTF_8);
	}

}

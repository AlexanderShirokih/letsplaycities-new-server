package ru.quandastudio.lpsserver.netty.handlers;

import java.util.List;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage;

@RequiredArgsConstructor
public class JsonDecoder extends ByteToMessageDecoder {
	private final Gson gson;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		int availableCapacity = in.readableBytes();
		ByteBuf buf = in.readBytes(availableCapacity);

		String data = buf.retain().toString(CharsetUtil.UTF_8);

		LPSClientMessage input = gson.fromJson(data, LPSClientMessage.class);
		
		out.add(input);
		buf.release();

	}

}

package ru.quandastudio.lpsserver.netty.handlers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.json.JsonObjectDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ProtocolDetector extends ByteToMessageDecoder {

	private final Gson gson;
	private final ServerHandler serverHandler;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		// Will use the first two bytes to detect a protocol.
		if (in.readableBytes() < 2) {
			return;
		}

		final int magic = in.getUnsignedShort(in.readerIndex());

		if (((magic >> 8) & 0xFF) == '{') {
			// This is a JSON message
			ChannelPipeline pipeline = ctx.pipeline();
			pipeline.addLast("decoder", new JsonObjectDecoder());
			pipeline.addLast("conventer", new JsonDecoder(gson));
			pipeline.addLast("handler", serverHandler);
			pipeline.addLast("encoder", new JsonEncoder(gson));
			pipeline.remove(this);
		} else if (magic == LPSv3Tags.LPS_CLIENT_VALID) {
			// This is a LPSv3 message
			ChannelPipeline pipeline = ctx.pipeline();
			pipeline.addLast("decoder", new LPSMessageDecoder());
			pipeline.addLast("handler", serverHandler);
			pipeline.addLast("encoder", new LPSMessageEncoder());
			pipeline.remove(this);
		} else {
			// Unknown protocol
			log.warn("Detected INVALID Message!");
			in.clear();
			ctx.close();
		}

	}

}

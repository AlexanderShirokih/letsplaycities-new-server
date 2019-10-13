package ru.quandastudio.lpsserver.protocol.lps;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import ru.quandastudio.lpsserver.protocol.LPSProtocolException;

public class LPSMessageWrapper {
	public enum TagType {
		INT, BIN, BUF
	}

	private ByteBuf buffer;

	private LPSMessageWrapper(ByteBuf buffer, boolean retain) {
		this.buffer = buffer;
		if (retain)
			buffer.retain();
	}

	public static LPSMessageWrapper newOutputMessage() {
		ByteBuf buffer = Unpooled.buffer();
		// Magic
		buffer.writeChar(LPSv3Tags.LPS_SERVER_VALID);
		// TagsCount
		buffer.writeByte(0);

		return new LPSMessageWrapper(buffer, true);
	}

	public static LPSMessageWrapper newInputMessage(ByteBuf buffer) {
		LPSMessageWrapper lpsmsg = new LPSMessageWrapper(buffer, false);
		return lpsmsg;
	}

	public boolean isValidLPSMessage() {
		char magic = buffer.getChar(0);
		if (magic != LPSv3Tags.LPS_CLIENT_VALID && magic != LPSv3Tags.LPS_SERVER_VALID)
			return false;
		int tagsCount = getTagsCount();
		int offset = 2;// Magic + CoT

		for (int i = 0; i < tagsCount; i++) {
			if (buffer.getByte(offset + 1) <= 0) {
				throw new LPSProtocolException("0 Tag");
			}
			int length = buffer.getChar(offset + 2);
			if (length < 0)
				throw new LPSProtocolException("Tag length < 0!");
			int idx = offset + 3 + length;

			if (idx > buffer.capacity())
				throw new IndexOutOfBoundsException("i=" + idx + "; cap=" + buffer.capacity());
			offset += length + 3;
		}

		return true;
	}

	public void release() {
		buffer.release();
	}

	public int getTagsCount() {
		return buffer.getUnsignedByte(2);
	}

	private void writeTagMeta(int tag, int length) {
		// Increase tags count
		buffer.setByte(2, getTagsCount() + 1);

		buffer.writeByte(tag);
		buffer.writeChar(length);
	}

	/*
	 * writeTag()
	 */

	public LPSMessageWrapper writeTag(int tag) {
		writeTagMeta(tag, 0);
		return this;
	}

	public LPSMessageWrapper writeTag(int tag, byte[] data) {
		writeTagMeta(tag, data.length);
		buffer.writeBytes(data);
		return this;
	}

	public LPSMessageWrapper writeTag(int tag, int data) {
		writeTagMeta(tag, 4);
		buffer.writeInt(data);
		return this;
	}

	public LPSMessageWrapper writeCharTag(int tag, int data) {
		writeTagMeta(tag, 2);
		buffer.writeChar(data);
		return this;
	}

	public LPSMessageWrapper writeByteTag(int tag, int data) {
		writeTagMeta(tag, 1);
		buffer.writeByte(data);
		return this;
	}

	public LPSMessageWrapper writeIntTag(int tag, int data) {
		writeTagMeta(tag, 4);
		buffer.writeInt(data);
		return this;
	}

	public LPSMessageWrapper writeLongTag(int tag, long data) {
		writeTagMeta(tag, 8);
		buffer.writeLong(data);
		return this;
	}

	public LPSMessageWrapper writeTag(int tag, UUID uuid) {
		writeTagMeta(tag, 16);
		buffer.writeLong(uuid.getMostSignificantBits());
		buffer.writeLong(uuid.getLeastSignificantBits());
		return this;
	}

	public LPSMessageWrapper writeTag(int tag, ByteBuf bb) {
		writeTagMeta(tag, bb.writerIndex());// + 1
		buffer.writeBytes(bb);
		return this;
	}

	public LPSMessageWrapper writeTag(int tag, String data) {
		byte[] bytes = data.getBytes();
		writeTag(tag, bytes);
		return this;
	}

	public LPSMessageWrapper writeTag(int tag, boolean data) {
		writeTagMeta(tag, 1);
		buffer.writeByte(data ? 1 : 0);
		return this;
	}

	/* */

	public ByteBuf getBuffer() {
		return buffer;
	}

	public void sendAndRelease(Channel channel) {
		if (channel != null && channel.isWritable()) {
			channel.writeAndFlush(getBuffer());
		}
		release();
	}

	public void sendAndRelease(ByteBuf out) {
		out.writeBytes(getBuffer());
		release();
	}

	private int scanOffset = 3;

	public int scanNextTag() {
		int len = buffer.getChar(scanOffset + 1);
		scanOffset += len + 3;
		if (buffer.capacity() <= scanOffset)
			return -1;
		return buffer.getByte(scanOffset);
	}

	/*
	 * readTag()
	 */

	public Object readTag(int rTag, TagType type) {
		int tagsCount = getTagsCount();
		int offset = 2;// Magic + CoT

		for (int i = 0; i < tagsCount; i++) {
			int tag = buffer.getByte(offset + 1);
			int length = buffer.getChar(offset + 2);
			if (tag == rTag) {
				int dataOffset = offset + 4;
				if (type == TagType.BIN) {
					byte[] data = new byte[length];
					buffer.getBytes(dataOffset, data);
					return data;
				} else if (type == TagType.BUF) {
					ByteBuf data = PooledByteBufAllocator.DEFAULT.buffer(16).retain();
					buffer.getBytes(dataOffset, data);
					return data;
				} else {
					switch (length) {
					case 1:
						return buffer.getByte(dataOffset);
					case 2:
						return buffer.getChar(dataOffset);
					case 4:
						return buffer.getInt(dataOffset);
					case 8:
						return buffer.getLong(dataOffset);
					default:
						throw new LPSProtocolException("Invalid message length for integer type. len=" + length);
					}
				}
			} else {
				offset += length + 3;
			}
		}
		return null;
	}

	public Integer readIntTag(int tag) {
		return (Integer) readTag(tag, TagType.INT);
	}

	public Character readCharTag(int tag) {
		return (Character) readTag(tag, TagType.INT);
	}

	public Byte readByteTag(int tag) {
		return (Byte) readTag(tag, TagType.INT);
	}

	public String readStringTag(int tag) {
		byte[] data = (byte[]) readTag(tag, TagType.BIN);
		return data == null ? null : new String(data);
	}

	public String readStringTag(int tag, int min, int max) {
		final String str = readStringTag(tag);
		if (str == null && min > 0)
			throw new LPSProtocolException("String tag is null, but it requested");
		int len = str.length();
		if (len < min || len > max)
			throw new LPSProtocolException(
					"checkLength() failed! actual length=" + len + "; [min=" + min + ";max=" + max + "]");
		return str;
	}

	public UUID readUUIDTag(int tag) {
		ByteBuf data = (ByteBuf) readTag(tag, TagType.BUF);
		if (data == null)
			return null;
		if (data.capacity() < 16) {
			data.release();
			return null;
		}
		long most = data.readLong();
		long least = data.readLong();
		data.release();
		return new UUID(most, least);
	}

	public boolean readBoolTag(int tag) {
		byte b = (byte) readTag(tag, TagType.INT);
		return b != 0;
	}

	/**
	 * Misc methods
	 */
	private int req;

	public void markRequestedTag() {
		req--;
		if (req < 0) {
			throw new LPSProtocolException("Requested tag marker less than 0!");
		}
	}

	public void setRequestedTagsCount(int req) {
		this.req = req;
	}

	public boolean isAllRequestedTagsExists() {
		return req == 0;
	}

	public int getMasterTag() {
		return buffer.getByte(3);
	}
}

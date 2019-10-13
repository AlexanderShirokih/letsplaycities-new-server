package ru.quandastudio.lpsserver.protocol.lps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.quandastudio.lpsserver.protocol.LPSProtocolException;

public class LPSMessageReader {

	private LPSTag[] tags;
	private int numTags;
	private int tagCntr;

	public LPSMessageReader(ByteBuf buf) throws IOException {
		char magic = buf.readChar();
		if (magic != LPSv3Tags.LPS_CLIENT_VALID) {
			throw new LPSProtocolException("Received an invalid LPS Message!");
		} else {
			numTags = buf.readUnsignedByte();
			tags = new LPSTag[numTags];
			readAllTags(buf);
		}
	}

	private void readAllTags(ByteBuf buf) throws IOException {
		for (int i = 0; i < numTags; i++) {
			int tag = buf.readUnsignedByte();
			if (tag == 0)
				throw new LPSProtocolException("LPS tag cannot be 0");
			int length = buf.readUnsignedShort();
			byte[] data = new byte[length];
			buf.readBytes(data);

			tags[i] = new LPSTag(tag, length, data);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LPSMessageReader [tags=");
		builder.append(Arrays.toString(tags));
		builder.append(", numTags=");
		builder.append(numTags);
		builder.append("]");
		return builder.toString();
	}

	public int nextTag() {
		if (tagCntr == numTags)
			return -1;
		return tags[tagCntr++].tag;
	}

	public int getMasterTag() {
		return tags[0].tag;
	}

	private Object getTag(int tag, LPSType type) {
		for (int i = 0; i < numTags; i++) {
			if (tags[i].tag == tag) {
				byte[] data = tags[i].data;
				Object val;
				switch (type) {
				case Bool:
					val = data[0] != 0;
					break;
				case Byte:
					val = Byte.valueOf(data[0]);
					break;
				case Char:
					val = ByteBuffer.wrap(data).getChar();
					break;
				case Int:
					val = ByteBuffer.wrap(data).getInt();
					break;
				case Long:
					val = ByteBuffer.wrap(data).getLong();
					break;
				case UUID:
					ByteBuffer bb = ByteBuffer.wrap(data);
					val = new UUID(bb.getLong(), bb.getLong());
					break;
				case Bin:
					val = data;
					break;
				case Buf:
					val = ByteBuffer.wrap(data);
					break;
				default:
					val = null;
				}
				return val;
			}
		}
		throw new LPSProtocolException("Tag '" + tag + "' not found!");
	}

	public boolean hasTag(int tag) {
		for (LPSTag t : tags)
			if (t.tag == tag)
				return true;
		return false;
	}

	public boolean readBoolean(int tag) {
		Boolean b = (Boolean) getTag(tag, LPSType.Bool);
		return b != null && b;
	}

	public Byte readByte(int tag) {
		Byte b = (Byte) getTag(tag, LPSType.Byte);
		return b == null ? 0 : b;
	}

	public int readChar(int tag) {
		Character i = (Character) getTag(tag, LPSType.Char);
		return i == null ? 0 : i.charValue();
	}

	public int readInt(int tag) {
		Integer i = (Integer) getTag(tag, LPSType.Int);
		return i == null ? 0 : i.intValue();
	}

	public long readLong(int tag) {
		Long l = (Long) getTag(tag, LPSType.Long);
		return l == null ? 0L : l.longValue();
	}

	public String readString(int tag) {
		byte[] data = (byte[]) getTag(tag, LPSType.Bin);
		return data == null ? null : new String(data);
	}

	public String readString(int tag, int min, int max) {
		String str = readString(tag);
		if (str == null && min > 0)
			throw new LPSProtocolException("String tag is null, but it requested");
		int len = str.length();
		if (len < min || len > max)
			throw new LPSProtocolException(
					"checkLength() failed! actual length=" + len + "; [min=" + min + ";max=" + max + "]");
		return str;
	}

	public UUID readUUID(int tag) {
		UUID uuid = (UUID) getTag(tag, LPSType.UUID);
		return uuid;
	}

	public byte[] readBytes(int tag) {
		byte[] data = (byte[]) getTag(tag, LPSType.Bin);
		return data;
	}

	public ByteBuffer readByteBuffer(int tag) {
		ByteBuffer buf = (ByteBuffer) getTag(tag, LPSType.Buf);
		return buf;
	}

	public boolean optBoolean(int tag) {
		return hasTag(tag) ? readBoolean(tag) : false;
	}

	public Byte optByte(int tag) {
		return hasTag(tag) ? readByte(tag) : 0;
	}

	public int optChar(int tag) {
		return hasTag(tag) ? readChar(tag) : 0;
	}

	public int optInt(int tag) {
		return hasTag(tag) ? readInt(tag) : 0;
	}

	public long optLong(int tag) {
		return hasTag(tag) ? readLong(tag) : 0;
	}

	public <T extends Enum<T>> T optEnum(Class<T> type, int tag) {
		T[] t = type.getEnumConstants();
		return hasTag(tag) ? t[readByte(tag)] : null;
	}

	public String optString(int tag) {
		return hasTag(tag) ? readString(tag) : null;
	}

	public String optString(int tag, int min, int max) {
		return hasTag(tag) ? readString(tag, min, max) : null;
	}

	public byte[] optBytes(int tag) {
		return hasTag(tag) ? readBytes(tag) : null;
	}

	private int reqTagsCount;

	public void setRequestedTagsCount(int reqTagsCount) {
		this.reqTagsCount = reqTagsCount;
	}

	public void markRequestedTag() {
		reqTagsCount--;
	}

	public boolean isAllRequestedTagsExists() {
		return reqTagsCount == 0;
	}

	public void clear() {
		for (LPSTag t : tags) {
			t.setData(null);
		}
		tags = null;
	}

	enum LPSType {
		Bool, Byte, Char, Int, Long, UUID, Bin, Buf
	}

	@AllArgsConstructor
	@Getter
	@Setter
	static class LPSTag {
		private final int tag;
		private final int length;
		private byte[] data;

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("TAG[tag=");
			builder.append(tag);
			builder.append(", data=");
			builder.append(data);
			builder.append("]");
			return builder.toString();
		}
	}
}

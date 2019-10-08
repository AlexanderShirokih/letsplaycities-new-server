package ru.quandastudio.lpsserver.protocol.lps;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class LPSMessageWriter {
	enum LPSTagType {
		BYTE, CHAR, INT, LONG, UUID, STRING, BIN
	}

	class LPSTag {
		int tag;
		Object content;
		LPSTagType type;
	}

	private DataOutputStream dos;
	private ArrayList<LPSTag> tags = new ArrayList<>();

	public LPSMessageWriter(DataOutputStream dos) {
		this.dos = dos;
	}

	private void addData(int tag, Object data, LPSTagType type) {
		LPSTag ltag = new LPSTag();
		ltag.content = data;
		ltag.tag = tag;
		ltag.type = type;

		tags.add(ltag);
		
	}

	private void writeHead() throws IOException {
		dos.writeChar(LPSv3Tags.LPS_CLIENT_VALID);
		dos.writeByte(tags.size());
	}

	private void writeTag(LPSTag t) throws IOException {
		Object obj = t.content;

		switch (t.type) {
		case BYTE:
			dos.writeChar(1);
			dos.writeByte((int) obj);
			break;
		case CHAR:
			dos.writeChar(2);
			dos.writeChar((int) obj);
			break;
		case INT:
			dos.writeChar(4);
			dos.writeInt((int) obj);
			break;
		case LONG:
			dos.writeChar(8);
			dos.writeLong((long) obj);
			break;
		case UUID:
			dos.writeChar(16);
			dos.writeLong(((UUID) obj).getMostSignificantBits());
			dos.writeLong(((UUID) obj).getLeastSignificantBits());
			break;
		case STRING:
			byte[] strData = ((String) obj).getBytes();
			dos.writeChar(strData.length);
			dos.write(strData);
			break;
		case BIN:
			byte[] data = ((byte[]) obj);
			dos.writeChar(data.length);
			dos.write(data);
			break;
		}

	}

	public void build() throws IOException {
		writeHead();

		for (LPSTag t : tags) {
			dos.writeByte(t.tag);
			writeTag(t);
		}
		tags.clear();
	}

	public void buildAndFlush() throws IOException {
		build();
		dos.flush();
	}
	public LPSMessageWriter writeBool(int tag, boolean data) {
		addData(tag, data ? 1 : 0, LPSTagType.BYTE);
		return this;
	}
	
	public LPSMessageWriter writeByte(int tag, int data) {
		addData(tag, data, LPSTagType.BYTE);
		return this;
	}

	public LPSMessageWriter writeChar(int tag, int data) {
		addData(tag, data, LPSTagType.CHAR);
		return this;
	}

	public LPSMessageWriter writeInt(int tag, int data) {
		addData(tag, data, LPSTagType.INT);
		return this;
	}

	public LPSMessageWriter writeLong(int tag, long data) {
		addData(tag, data, LPSTagType.LONG);
		return this;
	}

	public LPSMessageWriter writeUUID(int tag, UUID data) {
		addData(tag, data, LPSTagType.UUID);
		return this;
	}

	public LPSMessageWriter writeString(int tag, String data) {
		addData(tag, data, LPSTagType.STRING);
		return this;
	}

	public LPSMessageWriter writeBytes(int tag, byte[] data) {
		addData(tag, data, LPSTagType.BIN);
		return this;
	}

}

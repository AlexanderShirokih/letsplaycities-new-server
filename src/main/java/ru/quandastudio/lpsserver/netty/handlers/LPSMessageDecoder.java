package ru.quandastudio.lpsserver.netty.handlers;

import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACCESS_TOKEN;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACC_HASH;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_BAN;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_FIREBASE_TOKEN;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_FM_REQ_RESULT;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_FRIEND;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_LEAVE;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_LOGIN;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_MSG;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_PLAY;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_QUERY_BANLIST;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_QUERY_FRIEND_INFO;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_SPEC;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_WORD;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ALLOW_SEND_UID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.AVATAR_PART0;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.CAN_REC_MSG;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.CLIENT_BUILD;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.CLIENT_VERSION;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_ACCEPT_REQUSET;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_DELETE_REQUEST;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_DENY_REQUSET;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_FRIEND_MODE;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_QUERY_LIST;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_SEND_REQUEST;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.FIREBASE_TOKEN;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.FRIEND_UID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.OPP_UID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.SENDER_UID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.SN;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.SN_UID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.UID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.LOGIN;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import ru.quandastudio.lpsserver.netty.models.AuthType;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSBanList.LPSBanListBuilder;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.PlayMode;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.RequestType;
import ru.quandastudio.lpsserver.protocol.LPSProtocolException;
import ru.quandastudio.lpsserver.protocol.lps.LPSMessageReader;
import ru.quandastudio.lpsserver.util.StringUtil;

public class LPSMessageDecoder extends ReplayingDecoder<Void> {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		LPSMessageReader reader = null;
		try {
			reader = new LPSMessageReader(in);
		} catch (LPSProtocolException e) {
			in.clear();
			ctx.close();
		}
		LPSClientMessage message = processMessage(reader);
		reader.clear();
		out.add(message);
	}

	private LPSClientMessage processMessage(LPSMessageReader reader) {
		switch (reader.getMasterTag()) {
		case ACTION_LOGIN:
			return readLoginAction(reader);
		case ACTION_PLAY:
			return readPlayAction(reader);
		case ACTION_MSG:
			return readMsgAction(reader);
		case ACTION_WORD:
			return readWordAction(reader);
		case ACTION_BAN:
			return readBanAction(reader);
		case ACTION_LEAVE:
			return readLeaveAction(reader);
		case ACTION_FRIEND:
			return readFriendAction(reader);
		case ACTION_FM_REQ_RESULT:
			return readFriendModeRequest(reader);
		case ACTION_QUERY_BANLIST:
			return readQueryBanListAction(reader);
		case ACTION_QUERY_FRIEND_INFO:
			return readFriendInfoAction(reader);
		case ACTION_FIREBASE_TOKEN:
			return new LPSClientMessage.LPSUnknown("Called deprecated LPS action FirebaseToken. Ignore it");
		case ACTION_SPEC:
			return new LPSClientMessage.LPSUnknown("Warning! Unable to read Admin action on LPS Protocol");
		default:
			return new LPSClientMessage.LPSUnknown(
					"Received unknown LPS message with action: " + reader.getMasterTag());
		}
	}

	private LPSClientMessage readFriendInfoAction(LPSMessageReader reader) {
		return new LPSClientMessage.LPSFriendList();
	}

	private LPSClientMessage readQueryBanListAction(LPSMessageReader msg) {
		int type = msg.readByte(ACTION_QUERY_BANLIST);
		LPSBanListBuilder builder = LPSClientMessage.LPSBanList.builder();
		if (type == E_QUERY_LIST) {
			builder.type(RequestType.QUERY_LIST);
		} else if (type == E_DELETE_REQUEST)
			builder.type(RequestType.DELETE).friendUid(msg.readInt(FRIEND_UID));
		return builder.build();
	}

	private LPSClientMessage readFriendModeRequest(LPSMessageReader msg) {
		int res = msg.readByte(ACTION_FM_REQ_RESULT);
		int oppUid = msg.optInt(SENDER_UID);
		return new LPSClientMessage.LPSFriendMode(res, oppUid);
	}

	private LPSClientMessage readFriendAction(LPSMessageReader msg) {
		int opp_id = msg.optInt(FRIEND_UID);

		RequestType requestType = null;
		switch (msg.readByte(ACTION_FRIEND)) {
		case E_SEND_REQUEST:
			requestType = RequestType.SEND;
			break;
		case E_ACCEPT_REQUSET:
			requestType = RequestType.ACCEPT;
			break;
		case E_DENY_REQUSET:
			requestType = RequestType.DENY;
			break;
		case E_DELETE_REQUEST:
			requestType = RequestType.DELETE;
		}

		return new LPSClientMessage.LPSFriendAction(requestType, opp_id);
	}

	private LPSClientMessage readLeaveAction(LPSMessageReader reader) {
		return new LPSClientMessage.LPSLeave(null);
	}

	private LPSClientMessage readBanAction(LPSMessageReader msg) {
		return new LPSClientMessage.LPSBan();
	}

	private LPSClientMessage readLoginAction(LPSMessageReader msg) {
		LPSClientMessage.LPSLogIn login = new LPSClientMessage.LPSLogIn();
		login.setVersion(Integer.valueOf(msg.readByte(ACTION_LOGIN)));
		login.setLogin(msg.optString(LOGIN));
		login.setHash(msg.optString(ACC_HASH));
		login.setAccToken(msg.optString(ACCESS_TOKEN));
		login.setSnUID(msg.optString(SN_UID));
		login.setClientVersion(msg.readString(CLIENT_VERSION));
		login.setClientBuild(msg.optChar(CLIENT_BUILD));
		login.setFirebaseToken(msg.optString(FIREBASE_TOKEN));
		login.setCanReceiveMessages(msg.optBoolean(CAN_REC_MSG));
		login.setAllowSendUID(msg.optBoolean(ALLOW_SEND_UID));
		login.setUid(msg.optInt(UID));
		login.setAuthType(msg.optEnum(AuthType.class, SN));
		login.setAvatar(StringUtil.toBase64(msg.optBytes(AVATAR_PART0)));
		return login;
	}

	private LPSClientMessage readPlayAction(LPSMessageReader msg) {
		PlayMode pm = msg.readByte(ACTION_PLAY) == E_FRIEND_MODE ? PlayMode.FRIEND : PlayMode.RANDOM_PAIR;
		Integer oppUid = msg.optInt(OPP_UID);
		return new LPSClientMessage.LPSPlay(pm, oppUid);
	}

	private LPSClientMessage readMsgAction(LPSMessageReader msg) {
		return new LPSClientMessage.LPSMsg(msg.readString(ACTION_MSG, 1, 320));
	}

	private LPSClientMessage readWordAction(LPSMessageReader msg) {
		return new LPSClientMessage.LPSWord(msg.readString(ACTION_WORD, 1, 64));
	}

}

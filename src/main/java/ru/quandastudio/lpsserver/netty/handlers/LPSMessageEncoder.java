package ru.quandastudio.lpsserver.netty.handlers;

import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_BANNED;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_FRIEND_MODE_REQ;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_FRIEND_REQUEST;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_JOIN;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_LOGIN_RESULT;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_QUERY_BANLIST_RES;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_QUERY_FRIEND_RES;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.ACTION_TIMEOUT;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.CONNECTION_ERROR;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_FM_RESULT_BUSY;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_FM_RESULT_DENIED;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_FM_RESULT_NOT_FRIEND;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_FM_RESULT_OFFLINE;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_FRIEND_SAYS_NO;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_FRIEND_SAYS_YES;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.E_NEW_REQUEST;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.FRIEND_MODE_REQ_LOGIN;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.FRIEND_MODE_REQ_UID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.F_QUERY_NAMES;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.F_QUERY_USER_ACCEPT;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.F_QUERY_USER_IDS;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.MSG_OWNER;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.NEWER_BUILD;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.OPP_CLIENT_BUILD;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.OPP_CLIENT_VERSION;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.OPP_IS_FRIEND;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.OPP_LOGIN;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_ACC_HASH;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_ACTION_LEAVE;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_ACTION_MSG;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_ACTION_WORD;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_API_VERSION;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_AVATAR_PART0;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_BANNED_BY_OPP;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_CAN_REC_MSG;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_OPP_SN;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_OPP_SNUID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_OPP_UID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_OPP_UUID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.S_UID;
import static ru.quandastudio.lpsserver.protocol.lps.LPSv3Tags.WORD;

import java.util.List;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import ru.quandastudio.lpsserver.netty.models.BlackListItem;
import ru.quandastudio.lpsserver.netty.models.FriendInfo;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSBanned;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSBannedListMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSBannedMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSFriendModeRequest;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSFriendRequest;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSFriendsList;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSLeaveMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSLoggedIn;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSMsgMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSPlayMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSTimeoutMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSWordMessage;
import ru.quandastudio.lpsserver.protocol.LPSProtocolException;
import ru.quandastudio.lpsserver.protocol.lps.LPSMessageWrapper;

public class LPSMessageEncoder extends MessageToByteEncoder<LPSMessage> {

	@Override
	protected void encode(ChannelHandlerContext ctx, LPSMessage msg, ByteBuf out) throws Exception {
		if (msg instanceof LPSLoggedIn) {
			sendLoggedInAction((LPSLoggedIn) msg, out);
		} else if (msg instanceof LPSBanned) {
			sendLoginErrorAction((LPSBanned) msg, out);
		} else if (msg instanceof LPSPlayMessage) {
			sendPlayAction((LPSPlayMessage) msg, out);
		} else if (msg instanceof LPSWordMessage) {
			sendWordAction((LPSWordMessage) msg, out);
		} else if (msg instanceof LPSMsgMessage) {
			sendMsgAction((LPSMsgMessage) msg, out);
		} else if (msg instanceof LPSLeaveMessage) {
			sendLeaveAction((LPSLeaveMessage) msg, out);
		} else if (msg instanceof LPSTimeoutMessage) {
			sendTimeoutAction(out);
		} else if (msg instanceof LPSBannedMessage) {
			sendKickedMessage(out);
		} else if (msg instanceof LPSFriendRequest) {
			sendFriendRequest((LPSFriendRequest) msg, out);
		} else if (msg instanceof LPSFriendModeRequest) {
			sendFriendModeRequest((LPSFriendModeRequest) msg, out);
		} else if (msg instanceof LPSBannedListMessage) {
			sendBannedListMessage((LPSBannedListMessage) msg, out);
		} else if (msg instanceof LPSFriendsList) {
			sendFriendsListMessage((LPSFriendsList) msg, out);
		}
	}

	private void sendFriendsListMessage(LPSFriendsList friends, ByteBuf out) {
		List<FriendInfo> data = friends.getData();
		int size = data.size();
		LPSMessageWrapper msg = LPSMessageWrapper.newOutputMessage();
		msg.writeCharTag(ACTION_QUERY_FRIEND_RES, size);

		StringBuffer names = new StringBuffer();
		ByteBuf uids = Unpooled.buffer(size * 4);
		ByteBuf accept = Unpooled.buffer(size);

		for (FriendInfo info : data) {
			accept.writeBoolean(info.isAccepted());
			uids.writeInt(info.getUserId());
			names.append(info.getLogin());
			names.append("||");
		}
		msg.writeTag(F_QUERY_USER_IDS, uids);
		msg.writeTag(F_QUERY_NAMES, names.toString());
		msg.writeTag(F_QUERY_USER_ACCEPT, accept);
		msg.sendAndRelease(out);

		if (!uids.release() || !accept.release())
			throw new LPSProtocolException("can't release buffer");
	}

	private void sendBannedListMessage(LPSBannedListMessage banned, ByteBuf out) {
		List<BlackListItem> data = banned.getData();
		int size = data.size();
		LPSMessageWrapper msg = LPSMessageWrapper.newOutputMessage();
		msg.writeCharTag(ACTION_QUERY_BANLIST_RES, size);
		StringBuffer names = new StringBuffer();

		ByteBuf uids = Unpooled.buffer(size * 4);

		for (BlackListItem info : data) {
			uids.writeInt(info.getUserId());
			names.append(info.getLogin());
			names.append("||");
		}

		msg.writeTag(F_QUERY_USER_IDS, uids);
		msg.writeTag(F_QUERY_NAMES, names.toString());
		msg.sendAndRelease(out);

		if (!uids.release())
			throw new LPSProtocolException("can't release buffer");
	}

	private void sendFriendModeRequest(LPSFriendModeRequest msg, ByteBuf out) {
		int code = 0;
		switch (msg.getResult()) {
		case BUSY:
			code = E_FM_RESULT_BUSY;
			break;
		case OFFLINE:
			code = E_FM_RESULT_OFFLINE;
			break;
		case NOT_FRIEND:
			code = E_FM_RESULT_NOT_FRIEND;
			break;
		case DENIED:
			code = E_FM_RESULT_DENIED;
			break;
		default:
			break;
		}
		LPSMessageWrapper writer = LPSMessageWrapper.newOutputMessage();
		writer.writeByteTag(ACTION_FRIEND_MODE_REQ, code);

		if (msg.getLogin() != null) {
			writer.writeTag(FRIEND_MODE_REQ_LOGIN, msg.getLogin());
			writer.writeTag(FRIEND_MODE_REQ_UID, msg.getOppUid());
		}
		writer.sendAndRelease(out);
	}

	private void sendFriendRequest(LPSFriendRequest msg, ByteBuf out) {
		LPSMessageWrapper writer = LPSMessageWrapper.newOutputMessage();
		switch (msg.getResult()) {
		case ACCEPTED:
			writer.writeByteTag(ACTION_FRIEND_REQUEST, E_FRIEND_SAYS_YES);
			break;
		case DENIED:
			writer.writeByteTag(ACTION_FRIEND_REQUEST, E_FRIEND_SAYS_NO);
			break;
		case NEW_REQUEST:
			writer.writeByteTag(ACTION_FRIEND_REQUEST, E_NEW_REQUEST);
			break;
		}
		writer.sendAndRelease(out);
	}

	private void sendKickedMessage(ByteBuf out) {
		LPSMessageWrapper.newOutputMessage().writeByteTag(ACTION_BANNED, 0/* Ban */).sendAndRelease(out);
	}

	private void sendTimeoutAction(ByteBuf out) {
		LPSMessageWrapper.newOutputMessage().writeByteTag(ACTION_TIMEOUT, 0).sendAndRelease(out);
	}

	private void sendLeaveAction(LPSLeaveMessage msg, ByteBuf out) {
		LPSMessageWrapper.newOutputMessage().writeTag(S_ACTION_LEAVE, msg.isLeaved()).sendAndRelease(out);
	}

	private void sendMsgAction(LPSMsgMessage msg, ByteBuf out) {
		LPSMessageWrapper.newOutputMessage()
				.writeTag(S_ACTION_MSG, msg.getMsg())
				.writeTag(MSG_OWNER, false)
				.sendAndRelease(out);
	}

	private void sendWordAction(LPSWordMessage msg, ByteBuf out) {
		LPSMessageWrapper.newOutputMessage()
				.writeByteTag(S_ACTION_WORD, msg.getResult().ordinal())
				.writeTag(WORD, msg.getWord())
				.sendAndRelease(out);
	}

	private void sendPlayAction(LPSPlayMessage play, ByteBuf out) {
		LPSMessageWrapper msg = LPSMessageWrapper.newOutputMessage();
		msg.writeTag(ACTION_JOIN, play.getYouStarter())
				.writeTag(OPP_LOGIN, play.getLogin())
				.writeIntTag(S_OPP_UID, play.getOppUid())
				.writeTag(S_OPP_UUID, new UUID(0, play.getOppUid()))
				.writeTag(S_CAN_REC_MSG, play.getCanReceiveMessages())
				.writeCharTag(OPP_CLIENT_BUILD, play.getClientBuild())
				.writeTag(OPP_CLIENT_VERSION, play.getClientVersion())
				.writeTag(OPP_IS_FRIEND, play.isFriend())
				.writeTag(S_BANNED_BY_OPP, play.isBanned());

		if (!play.isBanned() && play.getAvatar() != null && play.getAvatar().length() > 0) {
			// NOTE: We can write only 64kB images
			msg.writeTag(S_AVATAR_PART0, play.getAvatar());
		}
		if (!play.isBanned() && play.isAllowSendSnUid()) {
			msg.writeByteTag(S_OPP_SN, play.getAuthType().ordinal());
			msg.writeTag(S_OPP_SNUID, play.getSnUID());
		}

		msg.sendAndRelease(out);
	}

	private void sendLoginErrorAction(LPSBanned msg, ByteBuf out) {
		LPSMessageWrapper.newOutputMessage()
				.writeTag(ACTION_LOGIN_RESULT, false)
				.writeTag(CONNECTION_ERROR, msg.getBanReason())
				.writeTag(S_API_VERSION, 4)
				.sendAndRelease(out);

	}

	private void sendLoggedInAction(LPSLoggedIn msg, ByteBuf out) {
		LPSMessageWrapper res = LPSMessageWrapper.newOutputMessage();
		res.writeTag(ACTION_LOGIN_RESULT, true);
		res.writeCharTag(NEWER_BUILD, msg.getNewerBuild());
		res.writeByteTag(S_API_VERSION, 4);
		res.writeIntTag(S_UID, msg.getUserId());
		res.writeTag(S_ACC_HASH, msg.getAccHash());
		res.sendAndRelease(out);
	}

}

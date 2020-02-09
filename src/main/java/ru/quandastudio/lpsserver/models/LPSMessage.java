package ru.quandastudio.lpsserver.models;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class LPSMessage {

	@Getter
	private final String action = getClass().getAnnotation(Action.class).value();

	private LPSMessage() {
	}

	@Action("logged_in")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSLoggedIn extends LPSMessage {
		@NonNull
		private final Integer newerBuild;
		@NonNull
		private final Integer userId;
		@NonNull
		private final String accHash;
		@NonNull
		private final String picHash;
	}

	@Action("login_error")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSBanned extends LPSMessage {
		private final String banReason;
		private final String connError;
	}

	@Action("join")
	@RequiredArgsConstructor
	@Getter
	@Setter
	public static final class LPSPlayMessage extends LPSMessage {
		private final AuthType authType;
		@NonNull
		private final String login;
		@Deprecated
		private String avatar = null;
		private String snUID = null;
		@NonNull
		private final Integer oppUid;
		@NonNull
		private final String clientVersion;
		@NonNull
		private final Integer clientBuild;
		@NonNull
		private final Boolean canReceiveMessages;
		private final boolean isFriend;
		@NonNull
		private final Boolean youStarter;
		private final boolean banned;

		private transient final boolean allowSendSnUid;
	}

	@Action("word")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSWordMessage extends LPSMessage {
		@NonNull
		private final WordResult result;
		@NonNull
		private final String word;
	}

	@Action("msg")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSMsgMessage extends LPSMessage {
		@NonNull
		private final String msg;
		private final boolean isSystemMsg;
	}

	@Action("leave")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSLeaveMessage extends LPSMessage {
		private final boolean leaved;
	}

	@Action("banned")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSBannedMessage extends LPSMessage {
		private final boolean isBannedBySystem;
		private final String description;

		public LPSBannedMessage() {
			this(true, "");
		}
	}

	@Action("banlist")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSBannedListMessage extends LPSMessage {
		private final List<BlackListItem> data;
	}

	@Action("friends")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSFriendsList extends LPSMessage {
		private final List<FriendInfo> data;
	}

	@Action("history")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSHistoryList extends LPSMessage {
		private final List<HistoryInfo> data;
	}

	@Action("fm_request")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSFriendModeRequest extends LPSMessage {
		private final String login = null;
		private final int oppUid;
		private final FriendModeResult result;
	}

	@Action("friend_request")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSFriendRequest extends LPSMessage {
		@NonNull
		private final FriendRequest result;
		@NonNull
		private final Integer uid;
		@NonNull
		private final String login;
	}

	@Action("timeout")
	public static final class LPSTimeoutMessage extends LPSMessage {
	}

	public enum FriendRequest {
		NEW_REQUEST, ACCEPTED, DENIED;
	}

}
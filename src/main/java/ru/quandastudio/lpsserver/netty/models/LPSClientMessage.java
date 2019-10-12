package ru.quandastudio.lpsserver.netty.models;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.quandastudio.lpsserver.validation.CheckInBanlist;

public class LPSClientMessage {
	private LPSClientMessage() {
	}

	@Getter
	private final String action = getClass().getAnnotation(Action.class).value();

	@Action("login")
	@Builder
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@Getter
	@ToString
	public static final class LPSLogIn extends LPSClientMessage {
		@NotNull
		@Min(value = 3, message = "Unsupported protocol version")
		@Max(value = 4, message = "Unsupported protocol version")
		private final Integer version;

		@NotNull
		@Size(min = 2, max = 64, message = "Login must be between 2 and 64 characters")
		@CheckInBanlist
		@Setter
		private String login;

		@NotNull
		private final AuthType authType;

		@NotNull
		private final Integer clientBuild;
		@NotNull
		@Size(max = 20, message = "Client version value is too long!")
		private final String clientVersion;
		@NotNull
		private final Boolean canReceiveMessages;

		@Size(max = 200)
		private final String firebaseToken;

		@Builder.Default
		private final int uid = 0;
		@Builder.Default
		@Size(max = 11, message = "Access hash value is too long!")
		private final String hash = null;
		@Builder.Default
		@Size(max = 64 * 1024)
		private final String avatar = null;
		@Builder.Default
		private final String accToken = null;
		@Builder.Default
		private final Boolean allowSendUID = false;
		@Builder.Default
		@Size(max = 32, message = "Social network UID is too long!")
		@Setter
		private String snUID = null;
	}

	public enum PlayMode {
		RANDOM_PAIR, FRIEND;
	}

	@Action("play")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSPlay extends LPSClientMessage {
		@NonNull
		private final PlayMode mode;
		private final int oppUid;
	}

	@Action("banlist")
	@Builder
	@Getter
	public static final class LPSBanList extends LPSClientMessage {
		@NonNull
		private final RequestType type;
		@Builder.Default
		private final int friendUid = 0;
	}

	@Action("friends_list")
	public static final class LPSFriendList extends LPSClientMessage {
	}

	public enum RequestType {
		QUERY_LIST, SEND, DELETE, ACCEPT, DENY;
	}

	@Action("friend")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSFriendAction extends LPSClientMessage {
		@NonNull
		private final RequestType type;
		private final int oppUid;
	}

	@Action("ban")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSBan extends LPSClientMessage {
		private final String type;
		private final int targetId = 0;

		public LPSBan() {
			this("report");
		}
	}

	@Action("word")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSWord extends LPSClientMessage {
		@NotNull(message = "Word shouldn't be NULL!")
		@Size(min = 2, max = 45, message = "Word size should be in range [2,45]")
		private final String word;
	}

	@Action("msg")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSMsg extends LPSClientMessage {
		@NonNull
		private final String msg;
	}

	@Action("fm_req_result")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSFriendMode extends LPSClientMessage {
		private final int result;
		private final int oppUid;
	}

	@Action("leave")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSLeave extends LPSClientMessage {
		private final String reason;
	}
	
	@Action("admin")
	@RequiredArgsConstructor
	@Getter
	public static final class LPSAdmin extends LPSClientMessage {
		@NonNull
		private final String command;
	}

	@RequiredArgsConstructor
	@Getter
	public static final class LPSUnknown extends LPSClientMessage {
		private final String description;
	}
}

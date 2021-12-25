package ru.quandastudio.lpsserver.models;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.quandastudio.lpsserver.data.entities.User;

import javax.validation.constraints.NotNull;

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

        @Deprecated
        private final Integer userId;

        @Deprecated
        private final String accHash;

        private final String picHash;
    }

    @Action("login_error")
    @RequiredArgsConstructor
    @Getter
    public static final class LPSBanned extends LPSMessage {
        private final String banReason;
        @Deprecated
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
        @Deprecated
        private String snUID = null;
        @NonNull
        private final Integer oppUid;
        @NonNull
        private final String clientVersion;
        @NonNull
        private final Integer clientBuild;
        @NonNull
        private final Boolean canReceiveMessages;
        @Deprecated
        private final boolean isFriend;
        @NotNull
        private final FriendshipStatus friendshipStatus;
        @NonNull
        private final Boolean youStarter;
        @Deprecated
        private final boolean banned;

        private final String pictureHash;
        @Deprecated
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
        @NonNull
        private final Integer ownerId;
    }

    @Action("msg")
    @RequiredArgsConstructor
    @Getter
    public static final class LPSMsgMessage extends LPSMessage {
        @NonNull
        private final String msg;
        private final boolean isSystemMsg;
        @NonNull
        private final Integer ownerId;
    }

    @Action("leave")
    @RequiredArgsConstructor
    @Getter
    public static final class LPSLeaveMessage extends LPSMessage {
        private final boolean leaved;
        @NonNull
        private final Integer ownerId;
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

    @Action("fm_request")
    @RequiredArgsConstructor
    @Getter
    public static final class LPSFriendModeRequest extends LPSMessage {
        private final String login;
        private final int oppUid;
        @NonNull
        private final FriendModeResult result;

        public LPSFriendModeRequest(User user, FriendModeResult result) {
            this(user.getName(), user.getId(), result);
        }
    }

    @Action("timeout")
    public static final class LPSTimeoutMessage extends LPSMessage {
    }

}
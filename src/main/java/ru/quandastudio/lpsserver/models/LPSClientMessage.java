package ru.quandastudio.lpsserver.models;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.quandastudio.lpsserver.validation.CheckInBanlist;

public class LPSClientMessage {

    @Getter
    private final String action = getClass().getAnnotation(Action.class).value();

    private LPSClientMessage() {
    }

    @Action("login")
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static final class LPSLogIn extends LPSClientMessage implements ISignInMessage {
        @NotNull
        @Min(value = 3, message = "Unsupported protocol version")
        @Max(value = 6, message = "Unsupported protocol version")
        private int version;

        @NotNull
        private int clientBuild;
        @NotNull
        @Size(max = 20, message = "Client version value is too long!")
        private String clientVersion;

        private Boolean canReceiveMessages = Boolean.TRUE;

        @Size(max = 200)
        private String firebaseToken;

        private int uid = 0;

        @Size(max = 11, message = "Access hash value is too long!")
        private String hash = null;

        @Deprecated(since = "1.5.0")
        @Size(min = 2, max = 64, message = "Login must be between 2 and 64 characters")
        @CheckInBanlist
        @Setter
        private String login;

        @Deprecated
        private AuthType authType = null;

        @Deprecated
        @Size(max = 64 * 1024)
        private String avatar = null;

        @Deprecated
        private String picHash;

        @Deprecated
        private String accToken = null;

        @Deprecated
        private Boolean allowSendUID = Boolean.FALSE;

        @Size(min = 2, max = 32, message = "Social network UID must be in size [2;32]!")
        @Setter
        @Deprecated
        private String snUID = null;
    }

    public enum PlayMode {
        RANDOM_PAIR, FRIEND, REFERAL,
    }

    @Action("play")
    @RequiredArgsConstructor
    @Getter
    public static final class LPSPlay extends LPSClientMessage {
        @NonNull
        private final PlayMode mode;

        /**
         * Used in friend game mode
         */
        private final int oppUid;
        /**
         * Used in referal game mode
         */
        private final long roomId;
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
}

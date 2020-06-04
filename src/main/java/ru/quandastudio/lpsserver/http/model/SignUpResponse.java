package ru.quandastudio.lpsserver.http.model;

import lombok.Getter;
import lombok.NonNull;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.AuthType;

@Getter
public class SignUpResponse {

    @NonNull
    private final Integer userId;

    @NonNull
    private final String accHash;

    @NonNull
    private final String name;

    @NonNull
    private final String state;

    @NonNull
    private final AuthType authType;

    private final String picHash;

    public SignUpResponse(final User user) {
        userId = user.getId();
        picHash = user.getAvatarHash();
        state = user.getRole().getLegacyName();
        name = user.getName();
        accHash = user.getAccessHash();
        authType = user.getAuthType();
    }

}

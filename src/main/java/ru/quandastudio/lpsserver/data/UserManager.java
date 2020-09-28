package ru.quandastudio.lpsserver.data;

import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.data.entities.ProfileView;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.http.model.SignUpRequest;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSLogIn;
import ru.quandastudio.lpsserver.models.ProfileInfo;

import java.util.Optional;

public interface UserManager {

    Result<User> logIn(LPSLogIn login);

    Result<User> logIn(SignUpRequest request);

    Result<User> getUserByIdAndHash(Integer userId, String accessHash);

    Optional<User> getUserById(Integer userId);

    /**
     * Returns main user profile info
     *
     * @param userId user id of which user we want to get info
     * @return Optional data containing user profile info
     */
    Optional<ProfileInfo> getUserProfileById(Integer userId);

    void setBanned(Integer userId, boolean isOn);

    void updateAvatarHash(User user, String hash);

    void updateFirebaseToken(User user, String token);

}

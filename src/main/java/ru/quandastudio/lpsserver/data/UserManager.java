package ru.quandastudio.lpsserver.data;

import java.util.Optional;

import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.http.model.SignUpRequest;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSLogIn;

public interface UserManager {

	Result<User> logIn(LPSLogIn login);

	Result<User> logIn(SignUpRequest request);

	Result<User> getUserByIdAndHash(Integer userId, String accessHash);

	Optional<User> getUserById(Integer userId);

	void setBanned(Integer userId, boolean isOn);

	void updateAvatarHash(User user, String hash);

	void updateFirebaseToken(User user, String token);

}

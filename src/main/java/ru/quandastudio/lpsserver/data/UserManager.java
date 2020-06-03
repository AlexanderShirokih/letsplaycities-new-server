package ru.quandastudio.lpsserver.data;

import java.util.Optional;

import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.data.entities.AuthData;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.http.model.SignUpRequest;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSLogIn;

public interface UserManager {

	public Result<AuthData> logIn(LPSLogIn login);

	public Result<AuthData> logIn(SignUpRequest request);

	public Result<User> getUserByIdAndHash(Integer userId, String accessHash);

	public Optional<AuthData> getAuthDataById(Integer userId);
	
	public Optional<User> getUserById(Integer userId);

	public void setBanned(Integer userId, boolean isOn);

	public void updateAvatarHash(User user, String hash);

	public void updateFirebaseToken(User user, String token);

	public String getFirebaseToken(User user);

}

package ru.quandastudio.lpsserver.data;

import java.util.Optional;

import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.http.model.SignUpRequest;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSLogIn;

public interface UserManager {

	public Result<User> logIn(LPSLogIn login);

	public Result<User> logIn(SignUpRequest request);

	public Result<User> getUserByIdAndHash(Integer userId, String accessHash);

	public Optional<User> getUserById(Integer userId);

	public void setBanned(Integer userId, boolean isOn);

	public void updateHash(User user, String hash);

}

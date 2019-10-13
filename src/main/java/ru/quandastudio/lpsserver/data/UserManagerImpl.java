package ru.quandastudio.lpsserver.data;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.SnManager;
import ru.quandastudio.lpsserver.data.dao.UserRepository;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.data.entities.User.State;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSLogIn;
import ru.quandastudio.lpsserver.util.StringUtil;

@Service
@Transactional
public class UserManagerImpl implements UserManager {

	@Autowired
	private UserRepository userDAO;

	public Result<User> logIn(LPSLogIn login) {
		if (login.getUid() > 0 && login.getHash() != null) {
			// Authorized user
			return processAuthorizedUser(login);
		} else if (login.getAuthType() != null && login.getAccToken() != null && login.getSnUID() != null
				&& login.getLogin() != null) {

			if (SnManager.validateAccessToken(login.getAuthType(), login.getAccToken(), login.getSnUID())) {
				// Unauthorized user or new user
				return processUnauthorizedUser(login);
			}
			return Result.error("Ошибка авторизации: Неверный токен доступа");
		}
		return Result.error("Not all requested tags was received!");
	}

	private Result<User> processAuthorizedUser(LPSLogIn login) {
		final Optional<User> user = userDAO.findByUserIdAndAccessId(login.getUid(), login.getHash());
		user.ifPresent((u) -> updateDataIfPresent(u, login));
		return Result.from(user, "Ошибка авторизации: Пользователь не найден или токены устарели");
	}

	private Result<User> processUnauthorizedUser(LPSLogIn login) {
		final Optional<User> user = userDAO.findBySnUidAndAuthType(login.getSnUID(), login.getAuthType().getName());

		user.ifPresent((u) -> updateDataIfPresent(u, login));

		return user.or(() -> {
			final User newUser = createUser(login);
			userDAO.save(newUser);
			return Optional.of(newUser);
		}).map((u) -> Result.success(u)).get();
	}

	private void updateDataIfPresent(User user, LPSLogIn login) {
		// Update login and fbToken
		user.setName(login.getLogin());
		user.setFirebaseToken(login.getFirebaseToken());
	}

	private User createUser(LPSLogIn login) {
		User user = new User();
		user.setAuthType(login.getAuthType().getName());
		user.setFirebaseToken(login.getFirebaseToken());
		user.setSnUid(login.getSnUID());
		user.setName(login.getLogin());
		user.setUserId(login.getUid());
		user.setAccessId(StringUtil.getAccIdHash());
		user.setState(State.ready);
		return user;
	}

	@Override
	public Optional<User> getUserById(Integer userId) {
		return userDAO.findById(userId);
	}

	@Override
	public void setBanned(Integer userId, boolean isOn) {
		userDAO.findById(userId).ifPresent((user) -> {
			user.setState(isOn ? State.banned : State.ready);
		});
	}

}

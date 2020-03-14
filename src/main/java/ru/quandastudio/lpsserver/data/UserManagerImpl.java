package ru.quandastudio.lpsserver.data;

import java.util.Objects;
import java.util.Optional;

import javax.transaction.Transactional;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.SnManager;
import ru.quandastudio.lpsserver.data.dao.PictureRepository;
import ru.quandastudio.lpsserver.data.dao.UserRepository;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.data.entities.User.State;
import ru.quandastudio.lpsserver.http.model.SignUpRequest;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSLogIn;
import ru.quandastudio.lpsserver.util.StringUtil;

@Service
@Transactional
public class UserManagerImpl implements UserManager {

	@Autowired
	private UserRepository userDAO;

	@Autowired
	private PictureRepository picturesDAO;

	@Override
	public Result<User> logIn(SignUpRequest request) {
		if (SnManager.validateAccessToken(request.getAuthType(), request.getAccToken(), request.getSnUID())) {
			final Optional<User> user = userDAO.findBySnUidAndAuthType(request.getSnUID(),
					request.getAuthType().getName());

			user.ifPresent((u) -> {
				u.setName(request.getLogin());
				u.setFirebaseToken(request.getFirebaseToken());
			});

			return user.or(() -> {
				User newUser = createUser(request);
				newUser = userDAO.save(newUser);
				return Optional.of(newUser);
			}).map((u) -> Result.success(u)).get();
		}
		return Result.error("Ошибка авторизации: Неверный токен доступа");
	}

	@Override
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
			User newUser = createUser(login);
			newUser = userDAO.save(newUser);
			if (newUser.getAvatarHash() != null)
				picturesDAO.save(new Picture(newUser, login.getAvatar().getBytes(), Picture.Type.BASE64));
			return Optional.of(newUser);
		}).map((u) -> Result.success(u)).get();
	}

	private void updateDataIfPresent(User user, LPSLogIn login) {
		// Update login, fbToken and avatar hash code
		user.setName(login.getLogin());
		user.setFirebaseToken(login.getFirebaseToken());
		if (login.getClientBuild() < 270) {
			final String hash = getHash(login);
			if (!Objects.equals(user.getAvatarHash(), hash) && hash != null) {
				if (user.getAvatarHash() == null) {
					picturesDAO.save(new Picture(user, login.getAvatar().getBytes(), Picture.Type.BASE64));
				} else
					picturesDAO.updateByOwner(user, login.getAvatar().getBytes());
				user.setAvatarHash(hash);
			}
		}
	}

	private String getHash(LPSLogIn login) {
		final String avatar = login.getAvatar();
		final boolean isAvatarPresent = avatar != null;
		return isAvatarPresent ? DigestUtils.md5Hex(avatar) : null;
	}

	private User createUser(LPSLogIn login) {
		User user = new User();
		user.setAuthType(login.getAuthType().getName());
		user.setFirebaseToken(login.getFirebaseToken());
		user.setSnUid(login.getSnUID());
		user.setName(login.getLogin());
		user.setAccessId(StringUtil.getAccIdHash());
		user.setState(State.ready);
		if (login.getClientBuild() < 270)
			user.setAvatarHash(getHash(login));
		return user;
	}

	private User createUser(SignUpRequest request) {
		User user = new User();
		user.setAuthType(request.getAuthType().getName());
		user.setFirebaseToken(request.getFirebaseToken());
		user.setSnUid(request.getSnUID());
		user.setName(request.getLogin());
		user.setAccessId(StringUtil.getAccIdHash());
		user.setState(State.ready);
		return user;
	}

	@Override
	public Result<User> getUserByIdAndHash(Integer userId, String accessHash) {
		return Result.empty()
				.check(() -> userId != null && accessHash != null && accessHash.length() == 8,
						"#010: Invalid authorization data")
				.flatMap((Object o) -> Result.from(userDAO.findByUserIdAndAccessId(userId, accessHash),
						String.format("#011: Authenification error %d:%s", userId, accessHash)))
				.check((User user) -> user.getState() != State.banned, "#012: Access error: User is banned!");
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

	@Override
	public void updateAvatarHash(User user, String hash) {
		userDAO.updateAvatarHash(user, hash);
	}

	@Override
	public void updateToken(User user, String token) {
		if (!token.isBlank() && token.length() <= 200) {
			userDAO.updateToken(user, token);
		}
	}

}

package ru.quandastudio.lpsserver.data;

import java.util.Objects;
import java.util.Optional;

import javax.transaction.Transactional;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.SnManager;
import ru.quandastudio.lpsserver.data.dao.AuthDataRepository;
import ru.quandastudio.lpsserver.data.dao.PictureRepository;
import ru.quandastudio.lpsserver.data.dao.UserRepository;
import ru.quandastudio.lpsserver.data.entities.AuthData;
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
	private AuthDataRepository authDataDAO;

	@Autowired
	private PictureRepository picturesDAO;

	@Override
	public Result<AuthData> logIn(SignUpRequest request) {
		if (SnManager.validateAccessToken(request.getAuthType(), request.getAccToken(), request.getSnUID())) {
			final Optional<AuthData> user = authDataDAO.findBySnUidAndAuthType(request.getSnUID(),
					request.getAuthType().getName());

			user.ifPresent((u) -> {
				u.getUser().setName(request.getLogin());
				u.setFirebaseToken(request.getFirebaseToken());
			});

			return user.or(() -> {
				AuthData authData = createAuthData(request);
				authDataDAO.save(authData);
				return Optional.of(authData);
			}).map((AuthData a) -> Result.success(a)).get();
		}
		return Result.error("Ошибка авторизации: Неверный токен доступа");
	}

	@Override
	public Result<AuthData> logIn(LPSLogIn login) {
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

	private Result<AuthData> processAuthorizedUser(LPSLogIn login) {
		final Optional<AuthData> authData = authDataDAO.findByUserAndAccessHash(login.getUid(), login.getHash());
		authData.ifPresent((a) -> updateDataIfPresent(a, login));
		return Result.from(authData, "Ошибка авторизации: Пользователь не найден или токены устарели");
	}

	private Result<AuthData> processUnauthorizedUser(LPSLogIn login) {
		final Optional<AuthData> authData = authDataDAO.findBySnUidAndAuthType(login.getSnUID(),
				login.getAuthType().getName());

		authData.ifPresent((a) -> updateDataIfPresent(a, login));

		return authData.or(() -> {
			AuthData newAuthData = createAuthData(login);
			authDataDAO.save(newAuthData);
			User newUser = newAuthData.getUser();
			if (newUser.getAvatarHash() != null)
				picturesDAO.save(new Picture(null, newUser, login.getAvatar().getBytes(), Picture.Type.BASE64));
			return Optional.of(newAuthData);
		}).map((u) -> Result.success(u)).get();
	}

	private void updateDataIfPresent(AuthData authData, LPSLogIn login) {
		// Update login, fbToken and avatar hash code
		User user = authData.getUser();
		user.setName(login.getLogin());
		authData.setFirebaseToken(login.getFirebaseToken());
		if (login.getClientBuild() < 270) {
			final String hash = getHash(login);
			if (!Objects.equals(user.getAvatarHash(), hash) && hash != null) {
				if (user.getAvatarHash() == null) {
					picturesDAO.save(new Picture(null, user, login.getAvatar().getBytes(), Picture.Type.BASE64));
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

	private AuthData createAuthData(LPSLogIn login) {
		AuthData authData = new AuthData();
		User user = new User();
		user.setName(login.getLogin());
		user.setState(State.ready);
		authData.setUser(user);
		authData.setFirebaseToken(login.getFirebaseToken());
		authData.setSnUid(login.getSnUID());
		authData.setAccessHash(StringUtil.getAccIdHash());
		authData.setAuthType(login.getAuthType().getName());
		if (login.getClientBuild() < 270)
			user.setAvatarHash(getHash(login));
		return authData;
	}

	private AuthData createAuthData(SignUpRequest request) {
		AuthData authData = new AuthData();
		User user = new User();
		user.setName(request.getLogin());
		user.setState(State.ready);
		authData.setUser(user);
		authData.setFirebaseToken(request.getFirebaseToken());
		authData.setSnUid(request.getSnUID());
		authData.setAccessHash(StringUtil.getAccIdHash());
		authData.setAuthType(request.getAuthType().getName());
		return authData;
	}

	@Override
	public Result<User> getUserByIdAndHash(Integer userId, String accessHash) {
		return Result.empty()
				.check(() -> userId != null && accessHash != null && accessHash.length() == 8,
						"#010: Invalid authorization data")
				.flatMap((Object o) -> Result.from(authDataDAO.findByUserAndAccessHash(userId, accessHash),
						String.format("#011: Authenification error %d:%s", userId, accessHash)))
				.map((AuthData auth) -> auth.getUser())
				.check((User user) -> user.getState() != State.banned, "#012: Access error: User is banned!");
	}

	@Override
	public Optional<User> getUserById(Integer userId) {
		return userDAO.findById(userId);
	}

	@Override
	public Optional<AuthData> getAuthDataById(Integer userId) {
		return authDataDAO.findByUser(userId);
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
	public void updateFirebaseToken(User user, String token) {
		if (!token.isBlank() && token.length() <= 200) {
			authDataDAO.updateFirebaseToken(user, token);
		}
	}

	@Override
	public String getFirebaseToken(User user) {
		return authDataDAO.findFirebaseTokenByUser(user);
	}
}

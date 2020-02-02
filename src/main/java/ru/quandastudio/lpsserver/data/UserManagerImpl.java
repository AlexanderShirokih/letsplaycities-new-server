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
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSLogIn;
import ru.quandastudio.lpsserver.util.StringUtil;

@Service
@Transactional
public class UserManagerImpl implements UserManager {

	@Autowired
	private UserRepository userDAO;

	@Autowired
	private PictureRepository picturesDAO;

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
				picturesDAO.save(new Picture(newUser, login.getAvatar()));
			return Optional.of(newUser);
		}).map((u) -> Result.success(u)).get();
	}

	private void updateDataIfPresent(User user, LPSLogIn login) {
		// Update login, fbToken and avatar hash code
		user.setName(login.getLogin());
		user.setFirebaseToken(login.getFirebaseToken());

		final String hash = getHash(login);
		if (!Objects.equals(user.getAvatarHash(), hash)) {
			if (hash == null)
				picturesDAO.deleteByOwner(user);
			else {
				if (user.getAvatarHash() == null) {
					picturesDAO.save(new Picture(user, login.getAvatar()));
				} else
					picturesDAO.updateByOwner(user, login.getAvatar().getBytes());
			}

			user.setAvatarHash(hash);
		}
	}

	private String getHash(LPSLogIn login) {
		final String avatar = login.getAvatar();
		return avatar == null ? null : DigestUtils.md5Hex(avatar);
	}

	private User createUser(LPSLogIn login) {
		User user = new User();
		user.setAuthType(login.getAuthType().getName());
		user.setFirebaseToken(login.getFirebaseToken());
		user.setSnUid(login.getSnUID());
		user.setName(login.getLogin());
		user.setAccessId(StringUtil.getAccIdHash());
		user.setState(State.ready);
		user.setAvatarHash(getHash(login));
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

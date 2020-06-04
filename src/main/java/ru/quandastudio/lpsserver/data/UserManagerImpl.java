package ru.quandastudio.lpsserver.data;

import java.util.Objects;
import java.util.Optional;

import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.SnManager;
import ru.quandastudio.lpsserver.data.dao.PictureRepository;
import ru.quandastudio.lpsserver.data.dao.UserRepository;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.data.entities.User.Role;
import ru.quandastudio.lpsserver.http.model.SignUpRequest;
import ru.quandastudio.lpsserver.models.ISignInMessage;
import ru.quandastudio.lpsserver.models.LPSClientMessage;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSLogIn;
import ru.quandastudio.lpsserver.util.StringUtil;

@Service
@Transactional
@RequiredArgsConstructor
public class UserManagerImpl implements UserManager {

    private final UserRepository userDAO;
    private final PictureRepository picturesDAO;

    @Override
    public Result<User> logIn(SignUpRequest request) {
        if (SnManager.validateAccessToken(request.getAuthType(), request.getAccToken(), request.getSnUID())) {
            final Optional<User> user = userDAO.findBySnUidAndAuthType(request.getSnUID(),
                    request.getAuthType());

            user.ifPresent((u) -> {
                u.setName(request.getLogin());
                u.setFirebaseToken(request.getFirebaseToken());
            });

            return user.or(() -> {
                User u = createUser(request);
                return Optional.of(userDAO.save(u));
            }).map(Result::success).orElseThrow();
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
        final Optional<User> authData = userDAO.findByIdAndAccessHash(login.getUid(), login.getHash());
        authData.ifPresent((a) -> updateDataIfPresent(a, login));
        return Result.from(authData, "Ошибка авторизации: Пользователь не найден или токены устарели");
    }

    private Result<User> processUnauthorizedUser(LPSLogIn login) {
        final Optional<User> authData = userDAO.findBySnUidAndAuthType(login.getSnUID(),
                login.getAuthType());

        authData.ifPresent((a) -> updateDataIfPresent(a, login));

        return authData.or(() -> {
            User user = createUser(login);
            User newUser = userDAO.save(user);
            if (newUser.getAvatarHash() != null)
                picturesDAO.save(new Picture(newUser, login.getAvatar().getBytes(), Picture.Type.BASE64));
            return Optional.of(newUser);
        }).map(Result::success).orElseThrow();
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

    private User createUser(ISignInMessage login) {
        User user = new User();
        user.setName(login.getLogin());
        user.setRole(Role.REGULAR_USER);
        user.setFirebaseToken(login.getFirebaseToken());
        user.setSnUid(login.getSnUID());
        user.setAccessHash(StringUtil.getAccIdHash());
        user.setAuthType(login.getAuthType());
        if (login instanceof LPSClientMessage.LPSLogIn && ((LPSLogIn) login).getClientBuild() < 270)
            user.setAvatarHash(getHash((LPSLogIn) login));
        return user;
    }

    @Override
    public Result<User> getUserByIdAndHash(Integer userId, String accessHash) {
        var res = userDAO.findByIdAndAccessHash(userId, accessHash);
        return Result.empty()
                .check(() -> userId != null && accessHash != null && accessHash.length() == 8,
                        "#010: Invalid authorization data")
                .flatMap((Object o) -> Result.from(res,
                        String.format("#011: Authenification error %d:%s", userId, accessHash)))
                .check((User user) -> user.getRole() != Role.BANNED_USER, "#012: Access error: User is banned!");
    }

    @Override
    public Optional<User> getUserById(Integer userId) {
        return userDAO.findById(userId);
    }

    @Override
    public void setBanned(Integer userId, boolean isOn) {
        userDAO.findById(userId).ifPresent((user) -> user.setRole(isOn ? Role.BANNED_USER : Role.REGULAR_USER));
    }

    @Override
    public void updateAvatarHash(User user, String hash) {
        userDAO.updateAvatarHash(user, hash);
    }

    @Override
    public void updateFirebaseToken(User user, String token) {
        if (!token.isEmpty() && token.length() <= 200) {
            userDAO.updateFirebaseToken(user.getId(), token);
        }
    }
}

package ru.quandastudio.lpsserver.actions;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.text.StringEscapeUtils;

import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.config.ServerProperties;
import ru.quandastudio.lpsserver.data.UserManager;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.data.entities.User.State;
import ru.quandastudio.lpsserver.netty.core.Player;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSLogIn;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;
import ru.quandastudio.lpsserver.util.StringUtil;

@Slf4j
@RequiredArgsConstructor
public class LoginAction {

	private final UserManager userManager;
	private static final String BAN_MSG = "Доступ к онлайн режиму ограничен. Вы заблокированы решением администрации.";

	private final Channel channel;

	public Optional<Player> logIn(LPSLogIn login) {
		Player player = new Player(channel);

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();

		Set<ConstraintViolation<LPSLogIn>> v = validator.validate(login);
		Iterator<ConstraintViolation<LPSLogIn>> validates = v.iterator();

		if (validates.hasNext()) {
			loginError(player, validates.next().getMessage());
			return Optional.empty();
		}

		login.setLogin(StringUtil.formatName(login.getLogin()));
		login.setSnUID(StringEscapeUtils.escapeJava(login.getSnUID()));

		Result<User> user = userManager.logIn(login);

		if (user.hasErrors()) {
			final String error = user.getError().getMessage();
			log.warn("LOGIN Error. MSG={}", error);
			loginError(player, error);
			return Optional.empty();
		}

		User userData = user.getData();

		if (userData.getState() == State.banned) {
			loginError(player, BAN_MSG);
			log.info("Logged BANNED player: {}; version={} by={}", player.getUser().getUserId(),
					player.getClientVersion(), player.getUser().getAuthType());
			return Optional.empty();
		}

		player.setUser(userData);

		player.setAllowSendUID(login.getAllowSendUID());
		player.setAvatarData(login.getAvatar());
		player.setCanReceiveMessages(login.getCanReceiveMessages());
		player.setClientBuild(login.getClientBuild());
		player.setClientVersion(login.getClientVersion());

		final Integer newerBuild = ServerProperties.getInstance().getNewerBuild();
		player.sendMessage(new LPSMessage.LPSLoggedIn(newerBuild, userData.getUserId(), userData.getAccessId()));

		if (ServerProperties.getInstance().isLoggingMsgsEnabled())
			log.info("Logged player: [{}, id={}]; version={}", userData.getName(), userData.getUserId(),
					player.getClientVersion());

		return Optional.of(player);
	}

	private void loginError(Player player, String errMsg) {
		player.sendMessage(new LPSMessage.LPSBanned(errMsg, null));
	}

}

package ru.quandastudio.lpsserver.actions;

import java.util.Optional;

import org.apache.commons.text.StringEscapeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.config.ServerProperties;
import ru.quandastudio.lpsserver.core.MessageChannel;
import ru.quandastudio.lpsserver.core.MessageRouter;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.UserManager;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.data.entities.User.State;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSLogIn;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;
import ru.quandastudio.lpsserver.util.StringUtil;
import ru.quandastudio.lpsserver.util.ValidationUtil;

@Slf4j
@RequiredArgsConstructor
public class LoginAction {

	private static final String BAN_MSG = "Доступ к онлайн режиму ограничен. Вы заблокированы решением администрации.";

	private final MessageChannel channel;
	private final MessageRouter handler;
	private final UserManager userManager;
	private final ServerContext serverContext;
	
	public Optional<Player> logIn(LPSLogIn login) {
		Player player = new Player(serverContext, channel, handler);

		final Optional<String> firstError = ValidationUtil.validateMessage(login);

		if (firstError.isPresent()) {
			loginError(player, firstError.get());
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

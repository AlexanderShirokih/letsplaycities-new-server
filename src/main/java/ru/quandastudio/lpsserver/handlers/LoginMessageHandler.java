package ru.quandastudio.lpsserver.handlers;

import java.util.Optional;

import org.apache.commons.text.StringEscapeUtils;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.AuthData;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.data.entities.User.State;
import ru.quandastudio.lpsserver.models.AuthType;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSLogIn;
import ru.quandastudio.lpsserver.models.LPSMessage;
import ru.quandastudio.lpsserver.util.StringUtil;
import ru.quandastudio.lpsserver.util.ValidationUtil;

@Slf4j
public class LoginMessageHandler extends MessageHandler<LPSLogIn> {

	public LoginMessageHandler() {
		super(LPSLogIn.class);
	}

	private static final String BAN_MSG = "Доступ к онлайн режиму ограничен. Вы заблокированы решением администрации.";

	@Override
	public boolean isAuthorizationRequired() {
		return false;
	}

	@Override
	public void handle(Player player, LPSLogIn login) {
		final Optional<String> firstError = ValidationUtil.validateMessage(login);

		if (firstError.isPresent()) {
			loginError(player, firstError.get());
			return;
		}

		login.setLogin(StringUtil.formatName(login.getLogin()));
		login.setSnUID(StringEscapeUtils.escapeJava(login.getSnUID()));

		final ServerContext context = player.getCurrentContext();

		Result<AuthData> authDataResult = context.getUserManager().logIn(login);

		if (authDataResult.hasErrors()) {
			final String error = authDataResult.getError().getMessage();
			log.warn("LOGIN Error. MSG={}", error);
			loginError(player, error);
			return;
		}

		AuthData authData = authDataResult.get();
		User user = authData.getUser();

		if (user.getState() == State.banned) {
			loginError(player, BAN_MSG);
			log.info("Logged BANNED player: {}; version={} by={}", player.getUser().getUserId(),
					player.getClientVersion(), player.getAuthType());
			return;
		}

		player.setUser(user);
		player.setAuthType(AuthType.from(authData.getAuthType()));
		player.setCanReceiveMessages(login.getCanReceiveMessages());
		player.setClientBuild(login.getClientBuild());
		player.setClientVersion(login.getClientVersion());

		final Integer newerBuild = context.getServerProperties().getNewerBuild();
		if (login.getVersion() >= 5)
			player.sendMessage(new LPSMessage.LPSLoggedIn(newerBuild, null, null, user.getAvatarHash()));
		else
			player.sendMessage(new LPSMessage.LPSLoggedIn(newerBuild, user.getUserId(), authData.getAccessHash(),
					user.getAvatarHash()));

		if (context.getServerProperties().isLoggingMsgsEnabled())
			log.info("Logged player: [{}, id={} sn={}, snUID={}]; version={}{}", user.getName(), user.getUserId(),
					authData.getAuthType(), authData.getSnUid(), player.getClientVersion(),
					player.hasAdminPrivilages() ? " as Admin" : "");
	}

	private void loginError(Player player, String errMsg) {
		player.sendMessage(new LPSMessage.LPSBanned(errMsg, null));
	}

}

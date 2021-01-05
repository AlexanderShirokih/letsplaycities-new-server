package ru.quandastudio.lpsserver.handlers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSLogIn;
import ru.quandastudio.lpsserver.models.LPSMessage;
import ru.quandastudio.lpsserver.models.Role;
import ru.quandastudio.lpsserver.util.StringUtil;
import ru.quandastudio.lpsserver.util.ValidationUtil;

import java.util.Optional;

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

        if (login.getLogin() != null) {
            login.setLogin(StringUtil.formatName(login.getLogin()));
        }

        login.setSnUID(StringEscapeUtils.escapeJava(login.getSnUID()));

        final ServerContext context = player.getCurrentContext();

        Result<User> userResult = context.getUserManager().logIn(login);

        if (userResult.hasErrors()) {
            final String error = userResult.getError().getMessage();
            log.warn("LOGIN Error. MSG={}", error);
            loginError(player, error);
            return;
        }

        User user = userResult.get();

        if (user.getRole() == Role.BANNED_USER) {
            loginError(player, BAN_MSG);
            log.info("Logged BANNED_USER player: {}; version={} by={}", player.getUser().getId(),
                    player.getClientVersion(), user.getAuthType());
            return;
        }

        player.setUser(user);
        player.setCanReceiveMessages(login.getCanReceiveMessages());
        player.setClientBuild(login.getClientBuild());
        player.setClientVersion(login.getClientVersion());

        final Integer newerBuild = context.getServerProperties().getNewerBuild();
        if (login.getVersion() >= 5)
            player.sendMessage(new LPSMessage.LPSLoggedIn(newerBuild, null, null, user.getAvatarHash()));
        else
            player.sendMessage(new LPSMessage.LPSLoggedIn(newerBuild, user.getId(), user.getAccessHash(),
                    user.getAvatarHash()));

        if (context.getServerProperties().isLoggingMsgsEnabled())
            log.info("Logged player: [{}, id={} sn={}, snUID={}]; version={}{}", user.getName(), user.getId(),
                    user.getAuthType(), user.getSnUid(), player.getClientVersion(),
                    player.hasAdminPrivileges() ? " as Admin" : "");
    }

    private void loginError(Player player, String errMsg) {
        player.sendMessage(new LPSMessage.LPSBanned(errMsg, null));
    }

}

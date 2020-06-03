package ru.quandastudio.lpsserver.handlers;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Banlist;
import ru.quandastudio.lpsserver.core.Dictionary;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSAdmin;

@Slf4j
public class AdminMessageHandler extends MessageHandler<LPSAdmin> {

	public AdminMessageHandler() {
		super(LPSAdmin.class);
	}

	@Override
	public void handle(Player player, LPSAdmin msg) {
		if (player.hasAdminPrivilages()) {
			handleAction(player, msg.getCommand().split(">"));
		}
	}

	private void handleAction(Player player, String[] split) {
		final ServerContext context = player.getCurrentContext();
		final Dictionary dictionary = context.getDictionary();

		switch (split[0]) {
		case "corr":
			if (split[1].equals("on")) {
				spec("Correction: ENABLED");
				dictionary.setCorrectionEnabled(true);
			}
			if (split[1].equals("off")) {
				spec("Correction: DISABLED");
				dictionary.setCorrectionEnabled(false);
			}
			break;
		case "reload":
			spec("Reloading database and configuration file...");
			Banlist.reloadBanDatabase();
			dictionary.reloadDictionary();
			context.getServerProperties().loadConfig();
			break;
		case "ban":
			boolean isOn = split[1].equals("on");
			Integer userId = Integer.parseInt(split[2]);
			spec(isOn ? "Ban" : "Unban" + " user with ID=" + userId);
			player.getCurrentContext().getUserManager().setBanned(userId, isOn);
			break;
		default:
			spec("Receiver unknown ADMIN command [" + split + "]");
		}
	}

	private void spec(String msg) {
		log.info("***SPEC: {}", msg);
	}

}

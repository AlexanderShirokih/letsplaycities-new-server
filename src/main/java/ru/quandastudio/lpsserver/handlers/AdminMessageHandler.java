package ru.quandastudio.lpsserver.handlers;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Banlist;
import ru.quandastudio.lpsserver.core.Dictionary;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSAdmin;

@Slf4j
public class AdminMessageHandler extends MessageHandler<LPSAdmin> {

	public AdminMessageHandler() {
		super(LPSAdmin.class);
	}

	@Override
	public void handle(Player player, LPSAdmin msg) {
		if (player.isAdmin()) {
			handleAction(player, msg.getAction().split(">"));
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
		}
	}

	private void spec(String msg) {
		log.info("***SPEC: {}", msg);
	}

}

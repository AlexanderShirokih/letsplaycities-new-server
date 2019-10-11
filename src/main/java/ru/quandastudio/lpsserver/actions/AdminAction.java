package ru.quandastudio.lpsserver.actions;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.config.ServerProperties;
import ru.quandastudio.lpsserver.core.Banlist;
import ru.quandastudio.lpsserver.core.Dictionary;

@Slf4j
public class AdminAction {

	public void onActionReceived(String spec) {
		if (spec != null) {
			String[] split = spec.split(">");
			handleAction(split);
		}
	}

	private void handleAction(String[] split) {
		switch (split[0]) {
		case "corr":
			if (split[1].equals("on")) {
				spec("Correction: ENABLED");
				Dictionary.setCorrectionEnabled(true);
			}
			if (split[1].equals("off")) {
				spec("Correction: DISABLED");
				Dictionary.setCorrectionEnabled(false);
			}
			break;
		case "reload":
			spec("Reloading database and configuration file...");
			Dictionary.reloadDictionary();
			Banlist.reloadBanDatabase();
			ServerProperties.reloadConfig();
			break;
		}
	}

	private void spec(String msg) {
		log.info("***SPEC: {}", msg);
	}
}

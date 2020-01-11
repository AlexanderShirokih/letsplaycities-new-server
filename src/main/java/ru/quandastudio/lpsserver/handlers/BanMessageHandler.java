package ru.quandastudio.lpsserver.handlers;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.models.LPSMessage;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSBan;

public class BanMessageHandler extends MessageHandler<LPSBan> {

	public BanMessageHandler() {
		super(LPSBan.class);
	}

	@Override
	public void handle(Player player, LPSBan msg) {
		final Room room = player.getRoom();

		if (room == null)
			return;

		Player p = room.oppositePlayer(player);

		if (player.checkVersion(190))
			player.getCurrentContext().getBanlistManager().addToBanlist(player.getUser(), p.getUser());

		if (p.isOnline()) {
			p.sendMessage(new LPSMessage.LPSBannedMessage());
		}
	}

}

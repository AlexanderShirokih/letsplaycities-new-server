package ru.quandastudio.lpsserver.core.handlers;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.data.BanlistManager;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSLeave;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;

@RequiredArgsConstructor
public class LeaveMessageHandler implements MessageHandler<LPSLeave> {

	private final boolean kicked;

	private final BanlistManager banlistManager;

	@Override
	public void handle(Player player, LPSLeave msg) {
		final Room room = player.getRoom();

		if (room == null)
			return;

		Player p = room.oppositePlayer(player);

		if (kicked && player.checkVersion(190))
			banlistManager.addToBanlist(player.getUser(), p.getUser());
		
		if (p.isOnline()) {
			p.sendMessage(kicked ? new LPSMessage.LPSBannedMessage() : new LPSMessage.LPSLeaveMessage(true));
		}
	}

}

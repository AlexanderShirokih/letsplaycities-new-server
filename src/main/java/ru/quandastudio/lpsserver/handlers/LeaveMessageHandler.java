package ru.quandastudio.lpsserver.handlers;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.models.LPSMessage;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSLeave;

public class LeaveMessageHandler extends MessageHandler<LPSLeave> {

	public LeaveMessageHandler() {
		super(LPSLeave.class);
	}

	@Override
	public void handle(Player player, LPSLeave msg) {
		final Room room = player.getRoom();

		if (room == null)
			return;

		Player p = room.oppositePlayer(player);

		if (p.isOnline()) {
			p.sendMessage(new LPSMessage.LPSLeaveMessage(true, player.getUser().getUserId()));
		}
	}

}

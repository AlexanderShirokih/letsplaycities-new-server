package ru.quandastudio.lpsserver.core.handlers;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSMsg;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;

public class UserMsgMessageHandler implements MessageHandler<LPSMsg> {

	@Override
	public void handle(Player player, LPSMsg msg) {
		final Room room = player.getRoom();

		if (room != null) {
			Player p = room.oppositePlayer(player);
			if (p.isOnline() && p.getCanReceiveMessages()) {
				p.sendMessage(new LPSMessage.LPSMsgMessage(msg.getMsg(), false));
			}
		}
	}

}

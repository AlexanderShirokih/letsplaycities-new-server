package ru.quandastudio.lpsserver.handlers;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.models.LPSMessage;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSMsg;

public class UserMsgMessageHandler extends MessageHandler<LPSMsg> {

	public UserMsgMessageHandler() {
		super(LPSMsg.class);
	}

	@Override
	public void handle(Player player, LPSMsg msg) {
		final Room room = player.getRoom();

		if (room != null) {
			Player p = room.oppositePlayer(player);
			if (p.isOnline() && p.getCanReceiveMessages()) {
				p.sendMessage(new LPSMessage.LPSMsgMessage(msg.getMsg(), false, player.getUser().getUserId()));
			}
		}
	}

}

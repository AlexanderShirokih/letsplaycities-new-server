package ru.quandastudio.lpsserver.core.handlers;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage;

public interface MessageHandler<T extends LPSClientMessage> {

	public void handle(Player player, T msg);

}

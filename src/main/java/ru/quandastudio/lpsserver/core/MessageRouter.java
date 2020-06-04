package ru.quandastudio.lpsserver.core;

import ru.quandastudio.lpsserver.models.LPSClientMessage;

public interface MessageRouter {

	void handleMessage(Player player, LPSClientMessage message);

}

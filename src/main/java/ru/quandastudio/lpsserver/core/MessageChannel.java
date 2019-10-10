package ru.quandastudio.lpsserver.core;

import ru.quandastudio.lpsserver.netty.models.LPSMessage;

public interface MessageChannel {

	public void send(LPSMessage message);

}

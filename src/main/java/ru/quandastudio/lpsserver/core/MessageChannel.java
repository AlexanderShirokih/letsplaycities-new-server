package ru.quandastudio.lpsserver.core;

import ru.quandastudio.lpsserver.models.LPSMessage;

public interface MessageChannel {

	public void send(LPSMessage message);

}

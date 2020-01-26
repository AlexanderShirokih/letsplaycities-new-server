package ru.quandastudio.lpsserver.core;

import ru.quandastudio.lpsserver.models.LPSMessage;

public interface MessageChannel {

	default public void send(LPSMessage message) {
	}

}

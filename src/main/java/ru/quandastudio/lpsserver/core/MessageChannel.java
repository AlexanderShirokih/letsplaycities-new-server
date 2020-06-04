package ru.quandastudio.lpsserver.core;

import ru.quandastudio.lpsserver.models.LPSMessage;

public interface MessageChannel {

	default void send(LPSMessage message) {
	}

}

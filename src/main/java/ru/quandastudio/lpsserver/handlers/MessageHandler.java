package ru.quandastudio.lpsserver.handlers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.models.LPSClientMessage;

@RequiredArgsConstructor
public abstract class MessageHandler<T extends LPSClientMessage> {

	@Getter
	private final Class<T> type;

	public abstract void handle(Player player, T msg);

	public boolean isAuthorizationRequired() {
		return true;
	}

}

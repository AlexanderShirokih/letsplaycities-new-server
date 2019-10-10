package ru.quandastudio.lpsserver.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.quandastudio.lpsserver.core.handlers.MessageHandler;
import ru.quandastudio.lpsserver.core.handlers.PlayMessageHandler;
import ru.quandastudio.lpsserver.data.BanlistManager;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSPlay;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MessageRouterImpl implements MessageRouter {

	@Autowired
	private BanlistManager banlistManager;

	private final Map<Class<? extends LPSClientMessage>, MessageHandler<? extends LPSClientMessage>> handlers;

	public MessageRouterImpl() {
		handlers = new HashMap<Class<? extends LPSClientMessage>, MessageHandler<? extends LPSClientMessage>>();
		handlers.put(LPSPlay.class, new PlayMessageHandler(banlistManager));
	}

	@Override
	public void handleMessage(Player player, LPSClientMessage msg) {
		@SuppressWarnings("unchecked")
		final MessageHandler<LPSClientMessage> handler = (MessageHandler<LPSClientMessage>) handlers
				.get(msg.getClass());
		if (handler == null) {
			throw new IllegalStateException("Handler for message type" + msg.getClass() + " is not found!");
		}

		handler.handle(player, msg);
	}

}

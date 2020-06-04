package ru.quandastudio.lpsserver.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.handlers.AdminMessageHandler;
import ru.quandastudio.lpsserver.handlers.BanMessageHandler;
import ru.quandastudio.lpsserver.handlers.BanlistMessageHandler;
import ru.quandastudio.lpsserver.handlers.FriendMessageHandler;
import ru.quandastudio.lpsserver.handlers.FriendModeRequestMessageHandler;
import ru.quandastudio.lpsserver.handlers.FriendsListMessageHandler;
import ru.quandastudio.lpsserver.handlers.HistoryMessageHandler;
import ru.quandastudio.lpsserver.handlers.LeaveMessageHandler;
import ru.quandastudio.lpsserver.handlers.LoginMessageHandler;
import ru.quandastudio.lpsserver.handlers.MessageHandler;
import ru.quandastudio.lpsserver.handlers.PlayMessageHandler;
import ru.quandastudio.lpsserver.handlers.UserMsgMessageHandler;
import ru.quandastudio.lpsserver.handlers.WordMessageHandler;
import ru.quandastudio.lpsserver.models.LPSClientMessage;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MessageRouterImpl implements MessageRouter {

	private final Map<Class<? extends LPSClientMessage>, MessageHandler<? extends LPSClientMessage>> handlers;

	public MessageRouterImpl() {
		handlers = new HashMap<>();

		registerTypeHandler(new LoginMessageHandler());
		registerTypeHandler(new PlayMessageHandler());
		registerTypeHandler(new AdminMessageHandler());
		registerTypeHandler(new LeaveMessageHandler());
		registerTypeHandler(new BanMessageHandler());
		registerTypeHandler(new BanlistMessageHandler());
		registerTypeHandler(new UserMsgMessageHandler());
		registerTypeHandler(new WordMessageHandler());
		registerTypeHandler(new FriendMessageHandler());
		registerTypeHandler(new FriendsListMessageHandler());
		registerTypeHandler(new FriendModeRequestMessageHandler());
		registerTypeHandler(new HistoryMessageHandler());
	}

	private void registerTypeHandler(MessageHandler<?> handler) {
		handlers.put(handler.getType(), handler);
	}

	@Override
	public void handleMessage(Player player, LPSClientMessage msg) {
		@SuppressWarnings("unchecked")
		final MessageHandler<LPSClientMessage> handler = (MessageHandler<LPSClientMessage>) handlers
				.get(msg.getClass());
		if (handler == null) {
			throw new IllegalStateException("Handler for message type" + msg.getClass() + " is not found!");
		}

		if (handler.isAuthorizationRequired() && !player.isAuthorized()) {
			log.warn("Can't handle message because user is not authorized! [user={}]", player.getUser());
			return;
		}

		handler.handle(player, msg);
	}

}

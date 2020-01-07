package ru.quandastudio.lpsserver.websocket;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.MessageCoder;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SocketHandler extends TextWebSocketHandler {
	private static ConcurrentHashMap<WebSocketSession, Player> players = new ConcurrentHashMap<WebSocketSession, Player>();

	private final MessageCoder messageCoder;

	private final ServerContext serverContext;

	public static void logstate() {
		log.info("WS: ONLINE: {}", players.size());
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws InterruptedException, IOException {

		Player p = Optional.ofNullable(players.get(session)).or(() -> {
			final Player newPlayer = new Player(serverContext, new WebSocketMessageChannel(session, messageCoder));
			players.put(session, newPlayer);
			return Optional.of(newPlayer);
		}).get();

		final LPSClientMessage msg = messageCoder.decode(message.getPayload());
		p.onMessage(msg);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		Player p = players.remove(session);
		if (p != null)
			p.onDisconnected();
	}
}

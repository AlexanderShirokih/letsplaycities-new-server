package ru.quandastudio.lpsserver.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.quandastudio.lpsserver.core.MessageCoder;
import ru.quandastudio.lpsserver.core.game.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.models.LPSClientMessage;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();

    private final MessageCoder messageCoder;

    private final ServerContext serverContext;

    public static void logstate() {
        log.info("WS: ONLINE: {}", players.size());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        Player p = Optional.ofNullable(players.get(session.getId())).or(() -> {
            final Player newPlayer = new Player(serverContext, new WebSocketMessageChannel(session, messageCoder));
            players.put(session.getId(), newPlayer);
            return Optional.of(newPlayer);
        }).get();

        final LPSClientMessage msg = messageCoder.decode(message.getPayload());
        p.onMessage(msg);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) {
        Player p = players.remove(session.getId());
        if (p != null)
            p.onDisconnected();
    }
}

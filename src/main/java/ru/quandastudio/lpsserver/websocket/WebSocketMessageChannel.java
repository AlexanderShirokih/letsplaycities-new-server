package ru.quandastudio.lpsserver.websocket;

import java.io.IOException;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.MessageChannel;
import ru.quandastudio.lpsserver.core.MessageCoder;
import ru.quandastudio.lpsserver.models.LPSMessage;

@AllArgsConstructor
@Slf4j
public class WebSocketMessageChannel implements MessageChannel {

	private final WebSocketSession session;

	private final MessageCoder messageCoder;

	@Override
	public void send(LPSMessage message) {
		if (session.isOpen()) {
			try {
				session.sendMessage(new TextMessage(messageCoder.encode(message)));
			} catch (IOException e) {
				log.warn("ERR sending websocket message: {}", e.toString());
				try {
					session.close();
				} catch (IOException e1) {
				}
			}
		}
	}

}

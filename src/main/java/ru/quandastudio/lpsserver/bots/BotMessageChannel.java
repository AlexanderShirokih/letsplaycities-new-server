package ru.quandastudio.lpsserver.bots;

import lombok.NonNull;
import lombok.Setter;
import ru.quandastudio.lpsserver.core.MessageChannel;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSPlayMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSWordMessage;

public class BotMessageChannel implements MessageChannel {

	@Setter
	@NonNull
	private Bot bot;

	@Override
	public void send(LPSMessage message) {
		if (bot != null) {
			if (message instanceof LPSWordMessage) {
				final LPSWordMessage wordMsg = (LPSWordMessage) message;
				bot.onWordResult(wordMsg.getWord(), wordMsg.getResult());
			} else if (message instanceof LPSPlayMessage) {
				final LPSPlayMessage playMsg = (LPSPlayMessage) message;
				bot.onPlay(playMsg.getYouStarter());
			}
		}
	}

}

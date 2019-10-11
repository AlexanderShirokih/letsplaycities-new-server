package ru.quandastudio.lpsserver.core.handlers;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSWord;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSMsgMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSWordMessage;
import ru.quandastudio.lpsserver.netty.models.WordResult;
import ru.quandastudio.lpsserver.util.ValidationUtil;

@Slf4j
public class WordMessageHandler implements MessageHandler<LPSWord> {

	@Override
	public void handle(Player player, LPSWord msg) {
		final Optional<String> firstError = ValidationUtil.validateMessage(msg);

		if (firstError.isPresent()) {
			player.sendMessage(new LPSWordMessage(WordResult.NO_WORD, msg.getWord()));
			player.sendMessage(new LPSMsgMessage(firstError.get(), true));
			return;
		}

		final Room room = player.getRoom();

		if (room != null)
			room.word(player, msg.getWord());
		else
			log.warn("*** Cathed exception! Room is null");
	}

}

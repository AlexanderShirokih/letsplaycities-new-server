package ru.quandastudio.lpsserver.handlers;

import java.util.List;
import java.util.stream.Collectors;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.HistoryProjection;
import ru.quandastudio.lpsserver.models.HistoryInfo;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSHistoryList;
import ru.quandastudio.lpsserver.models.LPSMessage;

public class HistoryMessageHandler extends MessageHandler<LPSHistoryList> {

	public HistoryMessageHandler() {
		super(LPSHistoryList.class);
	}

	@Override
	public void handle(Player player, LPSHistoryList msg) {
		final ServerContext context = player.getCurrentContext();

		final List<HistoryInfo> data = context.getHistoryManager()
				.getHistoryList(player.getUser())
				.stream()
				.map(HistoryInfo::new)
				.collect(Collectors.toList());

		player.sendMessage(new LPSMessage.LPSHistoryList(data));
	}

}

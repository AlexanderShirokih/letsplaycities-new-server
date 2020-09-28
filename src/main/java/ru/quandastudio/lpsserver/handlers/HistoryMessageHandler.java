package ru.quandastudio.lpsserver.handlers;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.models.HistoryInfo;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSHistoryList;
import ru.quandastudio.lpsserver.models.LPSMessage;

import java.util.List;

@Deprecated(forRemoval = true, since = "1.4.2")
public class HistoryMessageHandler extends MessageHandler<LPSHistoryList> {

    public HistoryMessageHandler() {
        super(LPSHistoryList.class);
    }

    @Override
    public void handle(Player player, LPSHistoryList msg) {
        final List<HistoryInfo> data = player
                .getCurrentContext()
                .getHistoryManager()
                .getHistoryList(player.getUser());

        player.sendMessage(new LPSMessage.LPSHistoryList(data));
    }

}

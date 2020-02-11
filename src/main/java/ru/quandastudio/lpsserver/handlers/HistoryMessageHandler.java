package ru.quandastudio.lpsserver.handlers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.HistoryItem;
import ru.quandastudio.lpsserver.data.entities.User;
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

		final User user = player.getUser();
		final List<HistoryItem> userHistory = context.getHistoryManager().getHistoryList(user);

		final List<User> other = userHistory.stream().map((HistoryItem item) -> {
			return (item.getInvited().getUserId() == user.getUserId()) ? item.getStarter() : item.getInvited();
		}).collect(Collectors.toList());

		final List<HistoryInfo> users = IntStream.range(0, userHistory.size())
				.mapToObj((int i) -> new HistoryInfo(other.get(i), userHistory.get(i)))
				.collect(Collectors.toList());

		final List<Friendship> friendsList = context.getFriendshipManager().getFriendsListIn(user, other);

		final List<HistoryInfo> data = users.stream()
				.map((HistoryInfo info) -> {
					final Integer uid = info.getUserId();
					for (Friendship f : friendsList) {
						if (f.getReceiver().getUserId() == uid || f.getSender().getUserId() == uid) {
							info.setFriend(true);
							break;
						}
					}
					return info;
				})
				.collect(Collectors.toList());

		player.sendMessage(new LPSMessage.LPSHistoryList(data));
	}

}

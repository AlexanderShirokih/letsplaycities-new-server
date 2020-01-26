package ru.quandastudio.lpsserver.handlers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.util.Pair;

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

		final List<Pair<User, HistoryInfo>> users = userHistory.stream().map((HistoryItem item) -> {
			User other = (item.getInvited().getUserId() == user.getUserId()) ? item.getStarter() : item.getInvited();
			HistoryInfo info = new HistoryInfo(other.getUserId(), other.getName(), item.getCreationDate().getTime(),
					item.getDuration(), item.getWordsCount());
			return Pair.of(other, info);
		}).collect(Collectors.toList());

		final List<User> other = users.parallelStream()
				.map((Pair<User, HistoryInfo> info) -> info.getFirst())
				.collect(Collectors.toList());

		final List<Friendship> friendsList = context.getFriendshipManager().getFriendsListIn(user, other);

		final List<HistoryInfo> data = users.stream()
				.map((Pair<User, HistoryInfo> info) -> info.getSecond())
				.map((HistoryInfo info) -> {
					boolean isFriend = false;
					for (Friendship f : friendsList) {
						final Integer uid = info.getUserId();
						if (f.getReceiver().getUserId() == uid || f.getSender().getUserId() == uid) {
							isFriend = true;
							break;
						}
					}
					info.setFriend(isFriend);
					return info;
				})
				.collect(Collectors.toList());
		player.sendMessage(new LPSMessage.LPSHistoryList(data));
	}

}

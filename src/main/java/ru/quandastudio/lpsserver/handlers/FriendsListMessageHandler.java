package ru.quandastudio.lpsserver.handlers;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.FriendshipManager;
import ru.quandastudio.lpsserver.models.FriendInfo;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSFriendList;
import ru.quandastudio.lpsserver.models.LPSMessage.LPSFriendsList;

import java.util.List;
import java.util.stream.Collectors;

@Deprecated(forRemoval = true)
public class FriendsListMessageHandler extends MessageHandler<LPSFriendList> {

	public FriendsListMessageHandler() {
		super(LPSFriendList.class);
	}

	@Override
	public void handle(Player player, LPSFriendList msg) {
		final ServerContext context = player.getCurrentContext();
		final FriendshipManager friendshipManager = context.getFriendshipManager();
		List<FriendInfo> friendInfoList = friendshipManager.getFriendsList(player.getUser())
				.stream()
				.map(FriendInfo::new)
				.collect(Collectors.toList());
		player.sendMessage(new LPSFriendsList(friendInfoList));
	}

}

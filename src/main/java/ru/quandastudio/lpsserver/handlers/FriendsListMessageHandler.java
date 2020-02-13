package ru.quandastudio.lpsserver.handlers;

import java.util.List;
import java.util.stream.Collectors;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.FriendshipManager;
import ru.quandastudio.lpsserver.data.entities.FriendshipProjection;
import ru.quandastudio.lpsserver.models.FriendInfo;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSFriendList;
import ru.quandastudio.lpsserver.models.LPSMessage.LPSFriendsList;

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
				.map((FriendshipProjection f) -> new FriendInfo(f))
				.collect(Collectors.toList());
		player.sendMessage(new LPSFriendsList(friendInfoList));
	}

}

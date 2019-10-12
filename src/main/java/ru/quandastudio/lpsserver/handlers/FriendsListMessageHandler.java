package ru.quandastudio.lpsserver.handlers;

import java.util.List;
import java.util.stream.Collectors;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.data.FriendshipManager;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.netty.models.FriendInfo;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSFriendList;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSFriendsList;

public class FriendsListMessageHandler extends MessageHandler<LPSFriendList> {

	public FriendsListMessageHandler() {
		super(LPSFriendList.class);
	}

	@Override
	public void handle(Player player, LPSFriendList msg) {
		final FriendshipManager friendshipManager = player.getCurrentContext().getFriendshipManager();
		final Integer playerId = player.getUser().getUserId();
		List<Friendship> friendsList = friendshipManager.getFriendsList(player.getUser());
		List<FriendInfo> friendInfoList = friendsList.stream()
				.map((fs) -> transform(playerId, fs))
				.collect(Collectors.toList());

		player.sendMessage(new LPSFriendsList(friendInfoList));
	}

	private FriendInfo transform(Integer playerId, Friendship fs) {
		final User oppUser = fs.getReceiver().getUserId() == playerId ? fs.getSender() : fs.getReceiver();
		return new FriendInfo(oppUser.getUserId(), oppUser.getName(), fs.getIsAccepted());
	}

}

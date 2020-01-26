package ru.quandastudio.lpsserver.handlers;

import java.util.List;
import java.util.stream.Collectors;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.FriendshipManager;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.FriendInfo;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSFriendList;
import ru.quandastudio.lpsserver.models.LPSMessage.LPSFriendsList;
import ru.quandastudio.lpsserver.models.PictureInfo;

public class FriendsListMessageHandler extends MessageHandler<LPSFriendList> {

	public FriendsListMessageHandler() {
		super(LPSFriendList.class);
	}

	@Override
	public void handle(Player player, LPSFriendList msg) {
		final ServerContext context = player.getCurrentContext();
		final FriendshipManager friendshipManager = context.getFriendshipManager();
		final Integer playerId = player.getUser().getUserId();
		List<Friendship> friendsList = friendshipManager.getFriendsList(player.getUser());
		List<FriendInfo> friendInfoList = friendsList.stream()
				.map((fs) -> transform(playerId, fs))
				.collect(Collectors.toList());

		List<User> users = friendInfoList.stream().map((fi) -> new User(fi.getUserId())).collect(Collectors.toList());

		List<PictureInfo> pics = context.getPictureManager()
				.getPicturesByUserId(users)
				.stream()
				.map((Picture pic) -> new PictureInfo(pic.getOwner().getUserId(), new String(pic.getImageData())))
				.collect(Collectors.toList());

		player.sendMessage(new LPSFriendsList(friendInfoList, pics));
	}

	private FriendInfo transform(Integer playerId, Friendship fs) {
		final User oppUser = fs.getReceiver().getUserId() == playerId ? fs.getSender() : fs.getReceiver();
		return new FriendInfo(oppUser.getUserId(), oppUser.getName(), fs.getIsAccepted());
	}

}

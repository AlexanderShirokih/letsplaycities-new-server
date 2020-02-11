package ru.quandastudio.lpsserver.handlers;

import java.util.List;
import java.util.stream.Collectors;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.data.entities.BannedUser;
import ru.quandastudio.lpsserver.models.BlackListItem;
import ru.quandastudio.lpsserver.models.LPSMessage;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSBanList;

public class BanlistMessageHandler extends MessageHandler<LPSBanList> {

	public BanlistMessageHandler() {
		super(LPSBanList.class);
	}

	@Override
	public void handle(Player player, LPSBanList msg) {
		switch (msg.getType()) {
		case QUERY_LIST:
			handleQueryList(player);
			break;
		case DELETE:
			handleDeleteAction(player, msg.getFriendUid());
			break;
		default:
			throw new IllegalStateException("Unsupported request type for ban message! type=" + msg.getType());

		}
	}

	private void handleQueryList(Player player) {
		final List<BannedUser> bannedList = player.getCurrentContext()
				.getBanlistManager()
				.getBannedUsers(player.getUser());
		final List<BlackListItem> blacklistItems = bannedList.stream()
				.map(BlackListItem::new)
				.collect(Collectors.toList());
		player.sendMessage(new LPSMessage.LPSBannedListMessage(blacklistItems));
	}
	
	private void handleDeleteAction(Player player, Integer bannedId) {
		player.getCurrentContext().getBanlistManager().removeFromBanlist(player.getUser().getUserId(), bannedId);
	}

}

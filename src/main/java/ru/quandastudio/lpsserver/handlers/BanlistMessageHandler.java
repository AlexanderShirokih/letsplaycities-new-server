package ru.quandastudio.lpsserver.handlers;

import java.util.List;
import java.util.stream.Collectors;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.data.entities.OppUserNameProjection;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.BlacklistWrapper;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSBanList;
import ru.quandastudio.lpsserver.models.LPSMessage;

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
			handleDeleteAction(player, new User(msg.getFriendUid()));
			break;
		default:
			throw new IllegalStateException("Unsupported request type for ban message! type=" + msg.getType());

		}
	}

	private void handleQueryList(Player player) {
		final List<BlacklistWrapper> blacklistItems = player.getCurrentContext()
				.getBanlistManager()
				.getBannedUsers(player.getUser())
				.stream()
				.map((OppUserNameProjection user) -> new BlacklistWrapper(user))
				.collect(Collectors.toList());
		
		player.sendMessage(new LPSMessage.LPSBannedListMessage(blacklistItems));
	}

	private void handleDeleteAction(Player player, User banned) {
		player.getCurrentContext().getBanlistManager().removeFromBanlist(player.getUser(), banned);
	}

}

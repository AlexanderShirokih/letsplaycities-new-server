package ru.quandastudio.lpsserver.actions;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.data.BanlistManager;
import ru.quandastudio.lpsserver.data.entities.BannedUser;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.netty.models.BlackListItem;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;

@RequiredArgsConstructor
public class BanListAction {

	private final BanlistManager banlistManager;
	private final Player p;

	public void onQuery() {
		final List<BannedUser> bannedList = banlistManager.getBannedUsers(p.getUser());
		final List<BlackListItem> blacklistItems = bannedList.stream()
				.map(this::transform)
				.collect(Collectors.toList());
		p.sendMessage(new LPSMessage.LPSBannedListMessage(blacklistItems));
	}

	private BlackListItem transform(BannedUser user) {
		return new BlackListItem(user.getBannedName(), user.getBannedId());
	}

	public void onRemoveFromBanlist(User banner, User banned) {
		banlistManager.removeFromBanlist(banner, banned);
	}
}

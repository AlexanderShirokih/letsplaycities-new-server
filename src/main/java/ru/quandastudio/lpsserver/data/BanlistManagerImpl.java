package ru.quandastudio.lpsserver.data;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.quandastudio.lpsserver.data.dao.BanlistDAO;
import ru.quandastudio.lpsserver.data.entities.BannedUser;
import ru.quandastudio.lpsserver.data.entities.User;

@Component
public class BanlistManagerImpl implements BanlistManager {

	@Autowired
	private BanlistDAO banlistDAO;

	@Override
	public void addToBanlist(User baner, User banned) {
		banlistDAO.addToBanlist(new BannedUser(baner.getUserId(), banned.getUserId(), banned.getName()));
	}

	@Override
	public void removeFromBanlist(User baner, User banned) {
		banlistDAO.removeFromBanlist(new BannedUser(baner.getUserId(), banned.getUserId(), null));
	}

	@Override
	public List<BannedUser> getBannedUsers(User banerId) {
		return banlistDAO.getBannedUsers(banerId.getUserId());
	}

	@Override
	public boolean isBanned(User user1, User user2) {
		return banlistDAO.isBanned(user1.getUserId(), user2.getUserId());
	}

}

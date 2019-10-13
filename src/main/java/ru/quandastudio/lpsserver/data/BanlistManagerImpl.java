package ru.quandastudio.lpsserver.data;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.quandastudio.lpsserver.data.dao.BannedUserRepository;
import ru.quandastudio.lpsserver.data.entities.BannedUser;
import ru.quandastudio.lpsserver.data.entities.User;

@Service
@Transactional
public class BanlistManagerImpl implements BanlistManager {

	@Autowired
	private BannedUserRepository banlistDAO;

	@Override
	public void addToBanlist(User baner, User banned) {
		banlistDAO.save(new BannedUser(baner.getUserId(), banned.getUserId(), banned.getName()));
	}

	@Override
	public void removeFromBanlist(Integer banerId, Integer bannedId) {
		banlistDAO.deleteByBanerIdAndBannedId(banerId, bannedId);
	}

	@Override
	public List<BannedUser> getBannedUsers(User banerId) {
		return banlistDAO.findByBanerIdOrBannedId(banerId.getUserId());
	}

	@Override
	public boolean isBanned(User user1, User user2) {
		return banlistDAO.existsByUserId(user1.getUserId(), user2.getUserId());
	}

}

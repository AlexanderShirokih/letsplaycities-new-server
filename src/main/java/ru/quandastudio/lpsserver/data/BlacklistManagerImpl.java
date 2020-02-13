package ru.quandastudio.lpsserver.data;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.quandastudio.lpsserver.data.dao.BlacklistRepository;
import ru.quandastudio.lpsserver.data.entities.BlackListUser;
import ru.quandastudio.lpsserver.data.entities.OppUserNameProjection;
import ru.quandastudio.lpsserver.data.entities.User;

@Service
@Transactional
public class BlacklistManagerImpl implements BlacklistManager {

	@Autowired
	private BlacklistRepository banlistDAO;

	@Override
	public void addToBanlist(User baner, User banned) {
		banlistDAO.save(new BlackListUser(baner, banned));
	}

	@Override
	public void removeFromBanlist(User baner, User banned) {
		banlistDAO.deleteByBanerAndBanned(baner, banned);
	}

	@Override
	public List<OppUserNameProjection> getBannedUsers(User banerId) {
		return banlistDAO.findBannedUsersByUser(banerId);
	}

	@Override
	public boolean isBanned(User user1, User user2) {
		return banlistDAO.existsByUser(user1, user2);
	}

}

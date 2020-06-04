package ru.quandastudio.lpsserver.data;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.quandastudio.lpsserver.data.dao.BlacklistRepository;
import ru.quandastudio.lpsserver.data.entities.Banlist;
import ru.quandastudio.lpsserver.data.entities.OppUserNameProjection;
import ru.quandastudio.lpsserver.data.entities.User;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BlacklistManagerImpl implements BlacklistManager {

	private final BlacklistRepository banlistDAO;

	@Override
	public void addToBanlist(User baner, User banned) {
		banlistDAO.save(new Banlist(baner, banned));
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

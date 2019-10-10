package ru.quandastudio.lpsserver.data.dao;

import java.util.List;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.BannedUser;

@Repository
public class BanlistDaoImpl extends BaseDAO implements BanlistDAO {

	@Override
	public void addToBanlist(BannedUser bannedUser) {
		save(bannedUser);
	}

	@Override
	public void removeFromBanlist(BannedUser bannedUser) {
		transactional((Session session) -> {
			session.delete(bannedUser);
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<BannedUser> getBannedUsers(Integer banerId) {
		return (List<BannedUser>) transactional((Session session) -> {
			return session.createQuery("from banlist where banerId=:banerId").setParameter("banerId", banerId).list();
		});
	}

}

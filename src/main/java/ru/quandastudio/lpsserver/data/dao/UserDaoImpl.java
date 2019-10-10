package ru.quandastudio.lpsserver.data.dao;

import java.util.Optional;

import javax.persistence.Query;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.netty.models.AuthType;

@Repository
public class UserDaoImpl extends BaseDAO implements UserDAO {

	@Override
	public void addUser(User user) {
		transactional((Session session) -> {
			session.save(user);
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional<User> getUserById(Integer userId, String accessHash) {
		return (Optional<User>) transactional((Session session) -> {
			Query query = session.createQuery("from User where userId = :userId and accessId = :accessId");
			query.setParameter("userId", userId);
			query.setParameter("accessId", accessHash);
			return query.getResultStream().findFirst();
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional<User> getUserBySnUID(String snUID, AuthType authType) {
		return (Optional<User>) transactional((Session session) -> {
			Query query = session.createQuery("from User where snUid = :snUid and authType = :authType");
			query.setParameter("snUid", snUID);
			query.setParameter("authType", authType.getName());
			return query.getResultStream().findFirst();
		});
	}

	@Override
	public void update(User user) {
		transactional((Session session) -> {
			session.update(user);
			return null;
		});
	}

}

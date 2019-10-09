package ru.quandastudio.lpsserver.data.dao;

import java.util.Optional;

import javax.persistence.Query;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.netty.models.AuthType;

@Repository
public class UserDaoImpl implements UserDAO {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public void addUser(User user) {
		Session session = sessionFactory.getCurrentSession();
		user.setUserId((Integer) session.save(user));
		session.getTransaction().commit();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional<User> getUserById(Integer userId, String accessHash) {
		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();
		Query query = sessionFactory.getCurrentSession()
				.createQuery("from User where userId = :userId and accessId = :accessId");
		query.setParameter("userId", userId);
		query.setParameter("accessId", accessHash);

		return (Optional<User>) query.getResultStream().findFirst();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional<User> getUserBySnUID(String snUID, AuthType authType) {
		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();
		Query query = sessionFactory.getCurrentSession()
				.createQuery("from User where snUid = :snUid and authType = :authType");
		query.setParameter("snUid", snUID);
		query.setParameter("authType", authType.getName());

		return (Optional<User>) query.getResultStream().findFirst();
	}

	@Override
	public void update(User user) {
		sessionFactory.getCurrentSession().update(user);
	}

}

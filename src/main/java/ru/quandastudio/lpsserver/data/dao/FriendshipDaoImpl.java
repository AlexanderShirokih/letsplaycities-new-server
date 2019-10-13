package ru.quandastudio.lpsserver.data.dao;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.User;

@Repository
public class FriendshipDaoImpl extends BaseDAO implements FriendshipDAO {

	@Override
	public void addNewRequest(Friendship friendship) {
		save(friendship);
	}

	@Override
	public void deleteFriend(User first, User second) {
		transactional((Session session) -> {
			session.delete(new Friendship(first, second));
			session.delete(new Friendship(second, first));
			return null;
		});

	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional<Friendship> getFriendsInfo(User first, User second) {
		return (Optional<Friendship>) transactional((Session session) -> {
			return session.createQuery(
					"from Friendship where (sender=:sender and receiver=:receiver) or (sender=:receiver and receiver=:sender)")
					.setParameter("sender", first)
					.setParameter("receiver", second)
					.uniqueResultOptional();
		});
	}

	@Override
	public void markAccepted(User sender, User receiver) {
		transactional((Session session) -> {
			session.update(new Friendship(null, sender, receiver, true, null));
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Friendship> getFriendsList(User user) {
		return (List<Friendship>) transactional((Session session) -> {
			return session
					.createQuery("from Friendship f where sender=:user or receiver=:user order by f.creationDate DESC")
					.setParameter("user", user)
					.list();
		});
	}

}

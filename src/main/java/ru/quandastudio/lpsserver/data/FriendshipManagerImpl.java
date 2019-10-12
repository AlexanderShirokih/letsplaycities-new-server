package ru.quandastudio.lpsserver.data;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.quandastudio.lpsserver.data.dao.FriendshipDAO;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.User;

@Component
public class FriendshipManagerImpl implements FriendshipManager {

	@Autowired
	private FriendshipDAO friendshipDAO;

	@Override
	public void addNewRequest(Friendship friendship) {
		friendshipDAO.addNewRequest(friendship);
	}

	@Override
	public void deleteFriend(User first, User second) {
		friendshipDAO.deleteFriend(first, second);
	}

	@Override
	public Optional<Friendship> getFriendsInfo(User first, User second) {
		return friendshipDAO.getFriendsInfo(first, second);
	}

	@Override
	public void markAccepted(User sender, User receiver) {
		friendshipDAO.markAccepted(sender, receiver);
	}

	@Override
	public List<Friendship> getFriendsList(User user) {
		return friendshipDAO.getFriendsList(user);
	}

}

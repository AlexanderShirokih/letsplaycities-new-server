package ru.quandastudio.lpsserver.data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.quandastudio.lpsserver.data.dao.FriendshipRepository;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.FriendshipProjection;
import ru.quandastudio.lpsserver.data.entities.User;

@Service
@Transactional
public class FriendshipManagerImpl implements FriendshipManager {

	@Autowired
	private FriendshipRepository friendshipDAO;

	@Override
	public void addNewRequest(Friendship friendship) {
		// Assert that we don't have any hanging request
		deleteFriend(friendship.getSender(), friendship.getReceiver());

		friendshipDAO.save(friendship);
	}

	@Override
	public void deleteFriend(User first, User second) {
		friendshipDAO.deleteBySenderAndReceiverOrReceiverAndSender(first, second);
	}

	@Override
	public Optional<Friendship> getFriendsInfo(User first, User second) {
		return friendshipDAO.findBySenderAndReceiverOrReceiverAndSender(first, second);
	}

	@Override
	public void markAcceptedIfExistsOrDelete(User sender, User receiver, boolean isAccepted) {
		final Optional<Friendship> friendInfo = friendshipDAO.findBySenderAndReceiverAndIsAcceptedFalse(sender, receiver);

		// Check if request exists or not
		friendInfo.ifPresent((Friendship info) -> {
			if (isAccepted && !info.getIsAccepted()) {
				info.setIsAccepted(true);
			} else {
				friendshipDAO.delete(info);
			}
		});
	}

	@Override
	public List<FriendshipProjection> getFriendsList(User user) {
		return friendshipDAO.findAllFriendsByUser(user);
	}

	@Override
	public List<Friendship> getFriendsListIn(User user, List<User> other) {
		return other.isEmpty() ? Collections.emptyList() : friendshipDAO.findBySenderAndReceiverIn(user, other);
	}

	@Override
	public void swapSenderAndReceiver(User newSender, User newReceiver) {
		friendshipDAO.swapSenderAndReceiver(newSender, newReceiver);
	}

	@Override
	public boolean isFriends(User user1, User user2) {
		return getFriendsInfo(user1, user2).map((info) -> info.getIsAccepted()).orElse(false);
	}

}

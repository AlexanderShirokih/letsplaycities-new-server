package ru.quandastudio.lpsserver.data;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.quandastudio.lpsserver.data.dao.FriendshipRepository;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.User;

@Service
@Transactional
public class FriendshipManagerImpl implements FriendshipManager {

	@Autowired
	private FriendshipRepository friendshipDAO;

	@Override
	public void addNewRequest(Friendship friendship) {
		// Assert that we don't have any handing request
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
	public void markAcceptedIfExistsOrDelete(User first, User second, boolean isAccepted) {
		final Optional<Friendship> friendInfo = friendshipDAO.findBySenderAndReceiver(first, second);

		// Check if request exists
		friendInfo.ifPresent((Friendship info) -> {
			if (isAccepted) {
				info.setIsAccepted(true);
			} else {
				friendshipDAO.delete(info);
			}
		});
	}

	@Override
	public List<Friendship> getFriendsList(User user) {
		return friendshipDAO.findAllBySenderOrReceiver(user);
	}

	@Override
	public void swapSenderAndReceiver(User newSender, User newReceiver) {
		friendshipDAO.swapSenderAndReceiver(newSender, newReceiver);
	}

}

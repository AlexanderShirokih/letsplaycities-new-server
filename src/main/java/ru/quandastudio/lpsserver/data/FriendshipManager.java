package ru.quandastudio.lpsserver.data;

import java.util.List;
import java.util.Optional;

import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.FriendshipProjection;
import ru.quandastudio.lpsserver.data.entities.User;

public interface FriendshipManager {

	void addNewRequest(Friendship friendship);

	void deleteFriend(User first, User second);

	Optional<Friendship> getFriendsInfo(User first, User second);

	List<FriendshipProjection> getFriendsList(User user);

	void markAcceptedIfExistsOrDelete(User sender, User receiver, boolean isAccepted);

	void swapSenderAndReceiver(User newSender, User newReceiver);
	
	boolean isFriends(User user1, User user2);
}

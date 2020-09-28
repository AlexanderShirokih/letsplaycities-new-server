package ru.quandastudio.lpsserver.data;

import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.FriendInfo;

import java.util.List;
import java.util.Optional;

public interface FriendshipManager {

	void addNewRequest(Friendship friendship);

	void deleteFriend(User first, User second);

	Optional<Friendship> getFriendsInfo(User first, User second);

	List<FriendInfo> getFriendsList(User user);

	void markAcceptedIfExistsOrDelete(User sender, User receiver, boolean isAccepted);

	void swapSenderAndReceiver(User newSender, User newReceiver);
	
	boolean isFriends(User user1, User user2);
}

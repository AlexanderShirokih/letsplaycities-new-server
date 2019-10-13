package ru.quandastudio.lpsserver.data;

import java.util.List;
import java.util.Optional;

import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.User;

public interface FriendshipManager {

	public void addNewRequest(Friendship friendship);

	public void deleteFriend(User first, User second);

	public Optional<Friendship> getFriendsInfo(User first, User second);

	public List<Friendship> getFriendsList(User user);
	
	public void markAcceptedIfExistsOrDelete(User first, User second, boolean isAccepted);
}

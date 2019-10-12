package ru.quandastudio.lpsserver.data.dao;

import java.util.List;
import java.util.Optional;

import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.User;

public interface FriendshipDAO {

	public void addNewRequest(Friendship friendship);

	public void deleteFriend(User first, User second);

	public Optional<Friendship> getFriendsInfo(User first, User second);

	public void markAccepted(User sender, User receiver);

	public List<Friendship> getFriendsList(User user);

}

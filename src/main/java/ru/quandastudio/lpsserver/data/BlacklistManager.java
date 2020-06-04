package ru.quandastudio.lpsserver.data;

import java.util.List;

import ru.quandastudio.lpsserver.data.entities.OppUserNameProjection;
import ru.quandastudio.lpsserver.data.entities.User;

public interface BlacklistManager {

	void addToBanlist(User baner, User banned);

	void removeFromBanlist(User baner, User banned);

	List<OppUserNameProjection> getBannedUsers(User baner);

	boolean isBanned(User user1, User user2);

}

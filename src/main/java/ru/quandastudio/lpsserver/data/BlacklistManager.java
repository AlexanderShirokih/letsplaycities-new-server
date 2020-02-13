package ru.quandastudio.lpsserver.data;

import java.util.List;

import ru.quandastudio.lpsserver.data.entities.OppUserNameProjection;
import ru.quandastudio.lpsserver.data.entities.User;

public interface BlacklistManager {

	public void addToBanlist(User baner, User banned);

	public void removeFromBanlist(User baner, User banned);

	public List<OppUserNameProjection> getBannedUsers(User baner);

	public boolean isBanned(User user1, User user2);

}

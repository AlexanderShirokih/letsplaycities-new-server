package ru.quandastudio.lpsserver.data;

import java.util.List;

import ru.quandastudio.lpsserver.data.entities.BannedUser;
import ru.quandastudio.lpsserver.data.entities.User;

public interface BanlistManager {

	public void addToBanlist(User baner, User banned);

	public void removeFromBanlist(Integer banerId, Integer bannedId);

	public List<BannedUser> getBannedUsers(User banerId);

	public boolean isBanned(User user1, User user2);

}

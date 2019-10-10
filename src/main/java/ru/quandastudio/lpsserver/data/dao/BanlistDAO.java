package ru.quandastudio.lpsserver.data.dao;

import java.util.List;

import ru.quandastudio.lpsserver.data.entities.BannedUser;

public interface BanlistDAO {

	public void addToBanlist(BannedUser bannedUser);

	public void removeFromBanlist(BannedUser bannedUser);

	public List<BannedUser> getBannedUsers(Integer banerId);

}

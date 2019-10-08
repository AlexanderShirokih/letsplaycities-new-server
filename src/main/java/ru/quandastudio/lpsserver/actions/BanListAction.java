package ru.quandastudio.lpsserver.actions;

import java.util.ArrayList;

import ru.quandastudio.lpsserver.netty.core.Player;

public class BanListAction {

	public static class BannedUser {
		public String login;
		public int userId;
	}

	private MySQLConnection db;
	private Player p;

	public BanListAction(Player p) {
		this.p = p;
		db = MySQLConnection.getInstance();
	}

	public void onQuery() {
		int bannerId = p.uid.getUserID();
		ArrayList<BannedUser> list = db.getBannedUsersList(bannerId);
		p.protocol.onBannedList(p, list);
		list.clear();
	}

	public void onRemoveFromBanlist(int bannerId, int bannedId) {
		db.removeFromBanList(bannerId, bannedId);
	}
}

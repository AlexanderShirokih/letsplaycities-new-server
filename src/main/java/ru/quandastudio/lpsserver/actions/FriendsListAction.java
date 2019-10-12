package ru.quandastudio.lpsserver.actions;

import java.util.ArrayList;

import ru.quandastudio.lpsserver.core.Player;


public class FriendsListAction {

	public static class FriendsInfo {
		public boolean accepted;
		public String login;
		public int userId;
	}

	private MySQLConnection db;
	private Player p;

	public FriendsListAction(Player p) {
		this.p = p;
		db = MySQLConnection.getInstance();
	}

	public void onQuery() {
		int senderId = p.uid.getUserID();
		ArrayList<FriendsInfo> list = db.getFriendsList(senderId);
		p.protocol.onFriendsList(p, list);
		list.clear();
	}
}

package ru.quandastudio.lpsserver.actions;

import ru.quandastudio.lpsserver.core.Player;

public class FriendsAction {
	public static final int NOT_FRIEND = -1;
	public static final int NOT_ACCEPTED = 0;
	public static final int ACCEPTED = 1;

	private MySQLConnection db;
	private Player player;
	private Player opp;
	private int oppId;

	public FriendsAction(Player p, int oppId) {
		this.player = p;
		this.oppId = oppId;
		db = MySQLConnection.getInstance();
		opp = player.getOppositePlayer();
		if (this.oppId == 0 && opp != null)
			this.oppId = opp.uid.getUserID();
	}

	/**
	 * Called when {@code player} sends new friend requset to {@code oppId}. If
	 * {@code oppId == 0} then will be used current opposite player. Sends
	 * Command.FRIEND if current opposite player is online.
	 */
	public void onNewRequest() {
		int senderId = player.uid.getUserID();
		if (db.getFriendRequestState(senderId, oppId) == NOT_FRIEND) {
			db.insertFriendRequest(senderId, oppId);
			if (opp != null)
				opp.protocol.sendCommand(opp, Command.FRIEND);
		}
	}

	/**
	 * Called when {@code player} accept or decline request sended by {@code oppId}.
	 * If {@code oppId == 0} then will be used current opposite player. Sends
	 * {@code Command.FRIEND_ACCEPTED} or {@code Command.FRIEND_DENIED} if current
	 * opposite player is online.
	 */
	public void onRequestResult(boolean isAccepted) {
		int receiverId = player.uid.getUserID();
		if (db.getFriendRequestState(oppId, receiverId) == NOT_ACCEPTED) {
			db.setFriendAcceptance(oppId, receiverId, isAccepted);

			if (opp != null) {
				if (isAccepted)
					opp.protocol.sendCommand(opp, Command.FRIEND_ACCEPTED);
				else
					opp.protocol.sendCommand(opp, Command.FRIENDS_DENIED);
			}
		}
	}

	/**
	 * Called when {@code player} wanna remove @{oppId} from friends list.
	 */
	public void onDeleteFriend() {
		int receiverId = player.uid.getUserID();
		if (!db.setFriendAcceptance(oppId, receiverId, false))
			db.setFriendAcceptance(receiverId, oppId, false);
	}

}

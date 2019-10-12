package ru.quandastudio.lpsserver.actions;


import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Player;

@Slf4j
public class FriendsRequestAction {
	
	private Player player;
	private MySQLConnection db;
	private int oppId;

	public FriendsRequestAction(Player player, int oppId) {
		this.player = player;
		this.oppId = oppId;
		this.db = MySQLConnection.getInstance();
	}

	public void onNewRequest() {
		if (isOppIsFriend()) {
			sendNotification();
			player.setToWaiting(oppId);
		} else
			player.protocol.onRequest(player, null, Command.FRIEND_MODE_NOT_FRIEND);
	}

	public void onRequestResult(boolean isAccepted) {
		if (isOppIsFriend()) {
			Player opp = Player.fromWaitingQueue(player.uid.getUserID(), oppId);
			if (opp == null)
				player.protocol.onRequest(player, opp, Command.FRIEND_MODE_OFFLINE);
			else {
				if (opp.getRoom() != null)
					player.protocol.onRequest(player, opp, Command.FRIEND_MODE_BUSY);
				else if (isAccepted) {
					opp.request(player);
				} else
					opp.protocol.onRequest(opp, player, Command.FRIEND_MODE_DENIED);
			}
		} else
			player.protocol.onRequest(player, null, Command.FRIEND_MODE_NOT_FRIEND);
	}

	private boolean isOppIsFriend() {
		int myId = player.uid.getUserID();
		return db.getFriendRequestState(myId, oppId) == ACCEPTED || db.getFriendRequestState(oppId, myId) == ACCEPTED;
	}

	private void sendNotification() {
		DbUserInfo user = db.getUserDataByUserID(oppId, null);
		if (user == null) {
			LOG.warn("# Can't send notification because user not found! user=" + oppId);
			player.protocol.onRequest(player, null, Command.FRIEND_MODE_DENIED);
			return;
		}
		if (user.firebaseToken != null) {
			LOG.info("Sending firebase request to user " + oppId);
			FirebaseNotifier.getInstance().sendNotification(player.uid.getUserID(), player.uid.getLogin(),
					user.firebaseToken);
		} else
			LOG.warn("# Can't send request for user " + oppId + ". Token not found");
	}
}

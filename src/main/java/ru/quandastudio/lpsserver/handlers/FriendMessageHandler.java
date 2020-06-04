package ru.quandastudio.lpsserver.handlers;

import java.util.HashMap;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.RequestNotifier.NotificationData;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.FriendshipManager;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSFriendAction;
import ru.quandastudio.lpsserver.models.LPSMessage.FriendRequest;
import ru.quandastudio.lpsserver.models.LPSMessage.LPSFriendRequest;

@Slf4j
public class FriendMessageHandler extends MessageHandler<LPSFriendAction> {

	public FriendMessageHandler() {
		super(LPSFriendAction.class);
	}

	@Override
	public void handle(Player player, LPSFriendAction msg) {
		switch (msg.getType()) {
		case SEND:
			handleSendRequest(player, msg.getOppUid());
			break;
		case ACCEPT:
			handleRequestResult(player, msg.getOppUid(), true);
			break;
		case DENY:
			handleRequestResult(player, msg.getOppUid(), false);
			break;
		case DELETE:
			handleDeleteRequest(player, msg.getOppUid());
			break;
		default:
			throw new IllegalStateException("Unsupported request type =" + msg.getType());
		}
	}

	/**
	 * Called when {@code player} sends new friend request to opposite player.
	 */
	private void handleSendRequest(Player player, Integer oppUid) {
		final ServerContext context = player.getCurrentContext();
		final FriendshipManager friendshipManager = context.getFriendshipManager();

		// If oppUid was specified we will use it, otherwise we will use current
		// opposite player
		Optional.ofNullable(oppUid)
				.filter((Integer opp) -> opp > 0)
				.map((Integer opp) -> Player.createDummyPlayer(context, opp))
				.or(player::getOppositePlayer)
				.ifPresentOrElse((Player oppPlayer) -> {
					final User oppUser = oppPlayer.getUser();
					final Optional<Friendship> friendInfo = friendshipManager.getFriendsInfo(player.getUser(), oppUser);

					friendInfo.ifPresentOrElse((info) -> {
						// We already have request
						if (!info.getIsAccepted()) {
							// That's case when we receive repeated request

							if (!info.getSender().equals(player.getUser())) {
								// Case of swapping receiver and sender.
								// We should swap their id's in database to be able to receive request result
								// from another user.
								friendshipManager.swapSenderAndReceiver(info.getReceiver(), info.getSender());
							}
							// Send notification again.
							sendNewRequestMessage(player, oppPlayer);
						}
					}, () -> {
						// Users are not friends. Send new request.
						friendshipManager.addNewRequest(new Friendship(player.getUser(), oppUser));
						sendNewRequestMessage(player, oppPlayer);
					});

				}, this::printNotPresentWarning);

	}

	private void sendNewRequestMessage(Player sender, Player oppPlayer) {
		final User senderUser = sender.getUser();
		if (oppPlayer.isOnline())// Normal player
			oppPlayer.sendMessage(
					new LPSFriendRequest(FriendRequest.NEW_REQUEST, senderUser.getId(), senderUser.getName()));
		else {
			// Dummy or offline player
			final ServerContext context = oppPlayer.getCurrentContext();
			final Integer oppId = oppPlayer.getUser().getId();
			final Optional<User> opp = context.getUserManager().getUserById(oppId);
			opp.ifPresentOrElse((authData) -> {
				final String firebaseToken = authData.getFirebaseToken();
				if (firebaseToken != null && !firebaseToken.isBlank()) {
					log.info("Sending firebase request to user {}", oppId);
					NotificationData data = buildNotificationData(sender.getUser());
					context.getRequestNotifier().sendNotification(authData, data);
				} else
					log.warn("# Can't send request for user {}. Token not found", oppId);
			}, () -> log.warn("# Can't send friend request notification because user not found! user={}", oppId));
		}
	}

	private NotificationData buildNotificationData(User sender) {
		final String title = "Пользователь " + sender.getName() + " хочет добавить вас в друзья.";
		final HashMap<String, String> params = new HashMap<>();

		params.put("action", "friend_request");
		params.put("result", "NEW_REQUEST");
		params.put("login", sender.getName());
		params.put("user_id", String.valueOf(sender.getId()));

		return new NotificationData(title, params);
	}

	/**
	 * Called when {@code player} accept or decline request sent by opposite player.
	 * @param player user who receives the request
	 * @param oppId user who sends the request
	 * 
	 */
	private void handleRequestResult(Player player, Integer oppId, boolean isAccepted) {
		final FriendshipManager friendshipManager = player.getCurrentContext().getFriendshipManager();
		final Optional<Player> oppPlayer = player.getOppositePlayer();
		final User pUser = player.getUser();
		final User oppUser = oppPlayer.map(Player::getUser).orElse(new User(oppId));

		friendshipManager.markAcceptedIfExistsOrDelete(oppUser, pUser, isAccepted);

		oppPlayer.ifPresent((opp) -> opp.sendMessage(new LPSFriendRequest(
				isAccepted ? FriendRequest.ACCEPTED : FriendRequest.DENIED, pUser.getId(), pUser.getName())));
	}

	/**
	 * Called when {@code player} wants to remove @{oppId} from friends list.
	 */
	private void handleDeleteRequest(Player player, Integer oppId) {
		final User first = player.getUser();
		final User second = player.getOppositePlayer().map(Player::getUser).orElse(new User(oppId));

		final FriendshipManager friendshipManager = player.getCurrentContext().getFriendshipManager();

		friendshipManager.deleteFriend(first, second);
	}

	private void printNotPresentWarning() {
		log.warn("Can't send friend request because opposite player is not present");
	}

}

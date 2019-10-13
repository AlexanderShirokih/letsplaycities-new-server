package ru.quandastudio.lpsserver.handlers;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.data.FriendshipManager;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSFriendAction;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.FriendRequest;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSFriendRequest;

@Slf4j
public class FriendMessageHandler extends MessageHandler<LPSFriendAction> {

	public FriendMessageHandler() {
		super(LPSFriendAction.class);
	}

	@Override
	public void handle(Player player, LPSFriendAction msg) {
		switch (msg.getType()) {
		case SEND:
			handleSendRequest(player);
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
	private void handleSendRequest(Player player) {
		final FriendshipManager friendshipManager = player.getCurrentContext().getFriendshipManager();

		player.getOppositePlayer().ifPresentOrElse((Player oppPlayer) -> {
			final User oppUser = oppPlayer.getUser();
			final Optional<Friendship> friendInfo = friendshipManager.getFriendsInfo(player.getUser(), oppUser);

			if (!friendInfo.isPresent()) {
				// Users are not friends. Send new request.
				friendshipManager.addNewRequest(new Friendship(player.getUser(), oppUser));
				oppPlayer.sendMessage(new LPSFriendRequest(FriendRequest.NEW_REQUEST));
			}

		}, this::printNotPresentWarning);

	}

	/**
	 * Called when {@code player} accept or decline request sent by opposite player.
	 */
	private void handleRequestResult(Player player, Integer oppId, boolean isAccepted) {
		final FriendshipManager friendshipManager = player.getCurrentContext().getFriendshipManager();
		final Optional<Player> oppPlayer = player.getOppositePlayer();
		final User oppUser = oppPlayer.map((opp) -> opp.getUser()).orElse(new User(oppId));

		friendshipManager.markAcceptedIfExistsOrDelete(player.getUser(), oppUser, isAccepted);

		oppPlayer.ifPresent((opp) -> opp
				.sendMessage(new LPSFriendRequest(isAccepted ? FriendRequest.ACCEPTED : FriendRequest.DENIED)));
	}

	/**
	 * Called when {@code player} wants to remove @{oppId} from friends list.
	 */
	private void handleDeleteRequest(Player player, Integer oppId) {
		final User first = player.getUser();
		final User second = player.getOppositePlayer().map((p) -> p.getUser()).orElse(new User(oppId));

		final FriendshipManager friendshipManager = player.getCurrentContext().getFriendshipManager();

		friendshipManager.deleteFriend(first, second);
	}

	private void printNotPresentWarning() {
		log.warn("Can't send friend request because opposite player is not present");
	}

}

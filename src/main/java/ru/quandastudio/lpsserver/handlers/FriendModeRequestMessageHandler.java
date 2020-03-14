package ru.quandastudio.lpsserver.handlers;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.data.entities.User.State;
import ru.quandastudio.lpsserver.models.FriendModeResult;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSFriendMode;
import ru.quandastudio.lpsserver.models.LPSMessage.LPSFriendModeRequest;

@Slf4j
public class FriendModeRequestMessageHandler extends MessageHandler<LPSFriendMode> {

	public FriendModeRequestMessageHandler() {
		super(LPSFriendMode.class);
	}

	@Override
	public void handle(Player player, LPSFriendMode msg) {
		switch (msg.getResult()) {
		case 1:
			handleRequestResult(player, msg.getOppUid(), true);
			break;
		case 2:
			handleRequestResult(player, msg.getOppUid(), false);
			break;
		default:
			log.warn("Unknown friend mode request result {}", msg.getResult());
		}
	}

	/**
	 * Handle request of {@code player}'s result, on {@code oppId}'s request
	 * 
	 * @param player     {@code Player} who sends response to oppId request.
	 * @param oppId      user's if who sends request to player
	 * @param isAccepted true if player accepts this request, false otherwise
	 */
	private void handleRequestResult(Player player, Integer oppId, boolean isAccepted) {
		final ServerContext context = player.getCurrentContext();

		Optional<User> opponent = context.getUserManager().getUserById(oppId);
		User oppUser = opponent.orElse(null);

		if (opponent.isEmpty() || !oppUser.isAtLeast(State.ready)) {
			player.sendMessage(new LPSFriendModeRequest(null, 0, FriendModeResult.NO_USER));
			return;
		}

		if (player.isFriend(oppId)) {
			final Optional<Player> optPlayer = context.popPlayerFromWaitingQueue(player.getUser(), oppUser);

			optPlayer.filter((p) -> p.getRoom() == null).ifPresentOrElse((Player requestSender) -> {
				if (isAccepted) {
					startFriendRoom(requestSender, player);
				} else {
					requestSender.sendMessage(new LPSFriendModeRequest(player.getUser(), FriendModeResult.DENIED));
				}
			}, () -> {
				player.sendMessage(new LPSFriendModeRequest(null, 0, FriendModeResult.NO_USER));
			});
		}

	}

	private void startFriendRoom(Player opp, Player player) {
		log.info("NEW FRIEND room: {} & {}", opp.getUser().getName(), player.getUser().getName());

		final ServerContext context = player.getCurrentContext();
		final Room room = new Room(player, opp);

		player.setRoom(room);
		opp.setRoom(room);

		room.start();

		context.getFriendsRequests().remove(player);
	}

}

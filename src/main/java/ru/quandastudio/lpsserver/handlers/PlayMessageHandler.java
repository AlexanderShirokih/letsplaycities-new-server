package ru.quandastudio.lpsserver.handlers;

import java.util.concurrent.ArrayBlockingQueue;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.netty.models.FriendModeResult;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSPlay;
import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSFriendModeRequest;

@Slf4j
public class PlayMessageHandler extends MessageHandler<LPSPlay> {

	public PlayMessageHandler() {
		super(LPSPlay.class);
	}

	@Override
	public void handle(Player player, LPSPlay msg) {
		switch (msg.getMode()) {
		case FRIEND:
			handleAsFriendsRequest(player, msg.getOppUid());
			break;
		case RANDOM_PAIR:
			handleAsRandomPlayRequest(player);
			break;
		default:
			throw new IllegalStateException("Unknown play mode " + msg.getMode().name());
		}
	}

	private void handleAsFriendsRequest(Player player, int oppUid) {
		if (player.isFriend(oppUid)) {
			sendNotification(player, oppUid);
			player.getCurrentContext().getFriendsRequests().put(player, oppUid);
		} else
			player.sendMessage(new LPSFriendModeRequest(0, FriendModeResult.NOT_FRIEND));
	}

	private void handleAsRandomPlayRequest(Player player) {
		ArrayBlockingQueue<Player> queue = player.getCurrentContext().getPlayersQueue();

		if (queue.isEmpty()) {
			queue.add(player);
		} else {
			Player p = null;
			while (!queue.isEmpty()) {
				p = queue.poll();
				if (!p.isOnline()) {
					p = null;
				} else
					break;
			}

			if (p == null) {
				queue.add(player);
				return;
			}

			if (player.getUser().equals(p.getUser())) {
				log.info("!! Dup UUID for {} & {}", p.getUser(), player.getUser());
				return;
			}

			log.info("NEW room: {} & {}", p.getUser().getName(), player.getUser().getName());

			final Room room = new Room(p, player);
			player.setRoom(room);
			p.setRoom(room);

			if (!room.start()) {
				log.info("Meeting of banned players: {} & {}", p.getUser().getName(), player.getUser().getName());
			}
		}
	}

	private void sendNotification(Player player, Integer oppId) {
		final ServerContext context = player.getCurrentContext();

		context.getUserManager().getUserById(oppId).ifPresentOrElse((user) -> {
			final String firebaseToken = user.getFirebaseToken();
			if (firebaseToken != null) {
				log.info("Sending firebase request to user {}", oppId);
				context.getRequestNotifier().sendNotification(player.getUser());
			} else
				log.warn("# Can't send request for user {}. Token not found", oppId);
		}, () -> {
			log.warn("# Can't send notification because user not found! user={}", oppId);
			player.sendMessage(new LPSFriendModeRequest(0, FriendModeResult.DENIED));
		});
	}

}

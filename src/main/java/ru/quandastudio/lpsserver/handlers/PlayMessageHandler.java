package ru.quandastudio.lpsserver.handlers;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.RequestNotifier.NotificationData;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.data.entities.User.State;
import ru.quandastudio.lpsserver.models.FriendModeResult;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSPlay;
import ru.quandastudio.lpsserver.models.LPSMessage.LPSFriendModeRequest;

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
		ServerContext context = player.getCurrentContext();

		User opponent = context.getUserManager()
				.getUserById(oppUid)
				.filter((User u) -> u.isAtLeast(State.ready))
				.orElse(null);

		if (opponent == null) {
			log.warn("# Can't send notification because user not found! user={}", oppUid);
			player.sendMessage(new LPSFriendModeRequest(null, 0,
					player.isAtLeastHasVersion(270) ? FriendModeResult.NO_USER : FriendModeResult.OFFLINE));
			return;
		}

		Player target = context.getPlayer(opponent).orElse(null);

		if (target != null && target.getRoom() != null) {
			player.sendMessage(new LPSFriendModeRequest(opponent, FriendModeResult.BUSY));
			return;
		}

		if (player.isFriend(oppUid)) {
			if (!sendNotification(player, opponent)) {
				player.sendMessage(new LPSFriendModeRequest(null, 0,
						player.isAtLeastHasVersion(270) ? FriendModeResult.NO_USER : FriendModeResult.OFFLINE));
			} else {
				context.getFriendsRequests().put(player, oppUid);
			}
		} else {
			player.sendMessage(new LPSFriendModeRequest(opponent, FriendModeResult.NOT_FRIEND));
		}
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

	private boolean sendNotification(Player player, User user) {
		final String firebaseToken = user.getFirebaseToken();
		if (firebaseToken != null) {
			log.info("Sending friend game request to user {}", user.getUserId());
			NotificationData data = buildNotificationData(player.getUser(), user);
			player.getCurrentContext().getRequestNotifier().sendNotification(user, data);
		} else {
			log.warn("# Can't send request for user {}. Token not found", user.getUserId());
			return false;
		}

		return true;
	}

	private NotificationData buildNotificationData(User sender, User receiver) {
		final String title = "Пользователь " + sender.getName() + " приглашает вас сыграть в города.";
		final HashMap<String, String> params = new HashMap<String, String>();

		params.put("action", "fm_request");
		params.put("login", sender.getName());
		params.put("user_id", String.valueOf(sender.getUserId()));
		params.put("target_id", String.valueOf(receiver.getUserId()));

		return new NotificationData(title, params);
	}

}

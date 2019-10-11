package ru.quandastudio.lpsserver.core.handlers;

import java.util.concurrent.ArrayBlockingQueue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.actions.FriendsRequestAction;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.data.BanlistManager;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSPlay;

@Slf4j
@RequiredArgsConstructor
public class PlayMessageHandler implements MessageHandler<LPSPlay> {

	private final BanlistManager banlistManager;

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
		new FriendsRequestAction(player, oppUid).onNewRequest();
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

			final Room room = new Room(banlistManager, p, player);
			player.setRoom(room);
			p.setRoom(room);

			if (!room.start()) {
				log.info("Meeting of banned players: {} & {}", p.getUser().getName(), player.getUser().getName());
			}
		}
	}

}

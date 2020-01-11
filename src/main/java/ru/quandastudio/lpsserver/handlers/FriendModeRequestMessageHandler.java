package ru.quandastudio.lpsserver.handlers;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.core.Room;
import ru.quandastudio.lpsserver.core.ServerContext;
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

	private void handleRequestResult(Player player, Integer oppId, boolean isAccepted) {
		if (player.isFriend(oppId)) {
			final Optional<Player> optPlayer = fromWaitingQueue(player, oppId);

			optPlayer.ifPresentOrElse((Player opp) -> {
				if (opp.getRoom() != null)
					player.sendMessage(new LPSFriendModeRequest(opp.getUser().getUserId(), FriendModeResult.BUSY));
				else if (isAccepted) {
					startFriendRoom(opp, player);
				} else
					opp.sendMessage(new LPSFriendModeRequest(player.getUser().getUserId(), FriendModeResult.DENIED));
			}, () -> player.sendMessage(new LPSFriendModeRequest(0, FriendModeResult.OFFLINE)));

		} else
			player.sendMessage(new LPSFriendModeRequest(0, FriendModeResult.NOT_FRIEND));
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

	private Optional<Player> fromWaitingQueue(Player player, Integer oppId) {
		Hashtable<Player, Integer> requests = player.getCurrentContext().getFriendsRequests();

		Iterator<Player> iter = requests.keySet().iterator();
		while (iter.hasNext()) {
			Player p = iter.next();
			if (!p.isOnline()) {
				iter.remove();
				continue;
			}
			if (p.getUser().getUserId() == oppId && requests.get(p) == player.getUser().getUserId()) {
				return Optional.of(p);
			}
		}
		return Optional.empty();
	}

}

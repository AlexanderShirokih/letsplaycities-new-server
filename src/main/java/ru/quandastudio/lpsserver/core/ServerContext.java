package ru.quandastudio.lpsserver.core;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Predicate;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.bots.BotManager;
import ru.quandastudio.lpsserver.config.ServerProperties;
import ru.quandastudio.lpsserver.data.BlacklistManager;
import ru.quandastudio.lpsserver.data.FriendshipManager;
import ru.quandastudio.lpsserver.data.HistoryManager;
import ru.quandastudio.lpsserver.data.PictureManager;
import ru.quandastudio.lpsserver.data.UserManager;
import ru.quandastudio.lpsserver.data.entities.User;

@Getter
@RequiredArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Slf4j
public class ServerContext {

	private final ArrayBlockingQueue<Player> playersQueue = new ArrayBlockingQueue<>(256);

	private final Hashtable<Player, Integer> friendsRequests = new Hashtable<>(256);

	private final BotManager botManager = new BotManager(this);

	private final MessageRouter messageRouter;

	private final UserManager userManager;

	private final BlacklistManager banlistManager;

	private final FriendshipManager friendshipManager;

	private final HistoryManager historyManager;

	private final PictureManager pictureManager;

	private final RequestNotifier requestNotifier;

	private final ServerProperties serverProperties;

	private final TaskLooper taskLooper;

	private final Dictionary dictionary;

	public void log() {
		log.info("QUEUE={}; WAITING_REQUESTS={}", playersQueue.size(), friendsRequests.size());
	}

	public Optional<Player> popPlayerFromWaitingQueue(User target, User sender) {
		int tId = target.getId();
		int sId = sender.getId();

		Iterator<Entry<Player, Integer>> iter = friendsRequests.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Player, Integer> entry = iter.next();
			Player p = entry.getKey();
			Integer targetId = entry.getValue();
			if (!p.isOnline()) {
				iter.remove();
				continue;
			}
			boolean senderMatches = p.getUser().getId().equals(sId);
			boolean targetMatches = targetId == tId;
			// Check that we have request with Player `sender` and target `target`
			if (senderMatches && targetMatches) {
				iter.remove();
				return Optional.of(p);
			}
		}
		return Optional.empty();
	}

	public Optional<Player> getPlayer(User user) {
		Predicate<Player> isTargetUser = (Player p) -> p.isOnline() && p.getUser().getId().equals(user.getId());

		return playersQueue.stream()
				.filter(isTargetUser)
				.findFirst()
				.or(() -> friendsRequests.keySet().stream().filter(isTargetUser).findFirst());
	}

}

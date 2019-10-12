package ru.quandastudio.lpsserver.core;

import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.config.ServerProperties;
import ru.quandastudio.lpsserver.data.BanlistManager;
import ru.quandastudio.lpsserver.data.FriendshipManager;
import ru.quandastudio.lpsserver.data.UserManager;

@Getter
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ServerContext {

	private ArrayBlockingQueue<Player> playersQueue = new ArrayBlockingQueue<Player>(256);

	private Hashtable<Player, Integer> friendsRequests = new Hashtable<>(256);

	private final MessageRouter messageRouter;

	private final UserManager userManager;

	private final BanlistManager banlistManager;

	private final FriendshipManager friendshipManager;

	private final RequestNotifier requestNotifier;

	private final ServerProperties serverProperties;
	
	private final TaskLooper taskLooper;

}

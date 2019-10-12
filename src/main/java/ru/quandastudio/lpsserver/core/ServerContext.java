package ru.quandastudio.lpsserver.core;

import java.util.concurrent.ArrayBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.config.ServerProperties;
import ru.quandastudio.lpsserver.data.BanlistManager;
import ru.quandastudio.lpsserver.data.UserManager;

@Getter
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ServerContext {

	@Getter
	private ArrayBlockingQueue<Player> playersQueue = new ArrayBlockingQueue<Player>(256);

	@NonNull
	private final MessageRouter messageRouter;

	@NonNull
	private final UserManager userManager;

	@NonNull
	private final BanlistManager banlistManager;

	@NonNull
	private final ServerProperties serverProperties;
}

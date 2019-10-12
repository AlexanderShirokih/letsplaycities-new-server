package ru.quandastudio.lpsserver.bots;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.LPSPlay;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage.PlayMode;

@Slf4j
@RequiredArgsConstructor
public class BotManager {

	private final ServerContext context;

	private boolean botsEnabled;

	private Bot[] bots;

	private ScheduledExecutorService executor;
	private ScheduledExecutorService botUpdaterExecutor;

	public void init() {
		botsEnabled = context.getServerProperties().isBotsEnabled();

		if (!botsEnabled)
			return;

		BotInfo[] infos = loadBotConfiguration();

		bots = new Bot[infos.length];

		for (int i = 0; i < bots.length; i++) {
			bots[i] = new Bot(context, infos[i]);
		}

		executor = Executors.newSingleThreadScheduledExecutor();
		botUpdaterExecutor = Executors.newSingleThreadScheduledExecutor();

		log.info("Initialized {} bots", bots.length);

		scheduleUpdate();
	}

	private void scheduleUpdate() {
		Runnable task = new Runnable() {

			@Override
			public void run() {
				try {
					update();
				} catch (Exception e) {
					log.error("Error in BOT update():", e);
				}
			}
		};
		botUpdaterExecutor.scheduleWithFixedDelay(task, 10, 10, TimeUnit.SECONDS);
	}

	public void update() {
		if (!botsEnabled)
			return;
		if (!isQueueEmpty()) {
			boolean chance = ThreadLocalRandom.current().nextBoolean();
			if (chance) {
				getFreeBot().ifPresent((bot) -> bot.onMessage(new LPSPlay(PlayMode.RANDOM_PAIR, 0)));
			}
		}
	}

	private boolean isQueueEmpty() {
		final Queue<?> playersQueue = context.getPlayersQueue();
		return playersQueue.isEmpty() || playersQueue.peek() instanceof Bot;
	}

	public void shutdown() {
		log.info("Shutting down BOT executor service...");
		if (executor != null)
			executor.shutdown();
		if (botUpdaterExecutor != null)
			botUpdaterExecutor.shutdown();
	}

	private BotInfo[] loadBotConfiguration() {
		BotInfo[] infos = null;
		try {
			final File file = new ClassPathResource("bots.json").getFile();
			infos = new BotConfigParser().parseBotsList(file);
			log.info("Bot list parsed");
		} catch (IOException e) {
			infos = new BotInfo[0];
			e.printStackTrace();
			log.info("Error parsing bot list!");
		}
		return infos;
	}

	public void shedule(BotGameSession bgs, int delay, Runnable task) {
		ScheduledFuture<?> futureTask = executor.schedule(task, delay, TimeUnit.MILLISECONDS);
		bgs.setFutureTask(futureTask);
	}

	public void log() {
		if (!botsEnabled)
			return;
		log.info("BOT stat: free {} of {}", getFreeBotList().size(), bots.length);
	}

	private List<Bot> getFreeBotList() {
		return Arrays.stream(bots).filter((b) -> b.isFree() || b.checkHangingTasks()).collect(Collectors.toList());
	}

	private Optional<Bot> getFreeBot() {
		List<Bot> freeBots = getFreeBotList();
		if (freeBots == null)
			return Optional.empty();
		return Optional
				.ofNullable(freeBots.get(ThreadLocalRandom.current().ints(0, freeBots.size()).findFirst().getAsInt()));
	}
}

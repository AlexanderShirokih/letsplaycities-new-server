package ru.quandastudio.lpsserver.bots;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.game.Player;
import ru.quandastudio.lpsserver.core.game.Room;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.AuthType;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSWord;
import ru.quandastudio.lpsserver.models.WordResult;

@Slf4j
public class Bot extends Player {

	private static final String BOT_VERSION = "v1.50_bot";
	private static final int BOT_VERSION_CODE = 133;

	@Getter
	private final int min;

	@Getter
	private final int max;

	private boolean isFree = true;
	private long lastTime;

	private final BotGameSession session;

	public Bot(ServerContext context, BotInfo info) {
		super(context, new BotMessageChannel());

		((BotMessageChannel) getChannel()).setBot(this);

		User user = new User(info.user_id);
		user.setName(info.name);
		user.setAuthType(AuthType.Native);
		setUser(user);
//		setAvatarData(StringUtil.toBase64(info.avatar));
		setClientBuild(BOT_VERSION_CODE);
		setClientVersion(BOT_VERSION);
		setCanReceiveMessages(false);
		min = info.minWaitingTimeMs;
		max = info.maxWaitingTimeMs;
		session = new BotGameSession(this, (byte) info.wordsLevel, info.prefCountries);
	}

	public boolean isFree() {
		return isFree || checkHangingTasks();
	}

	/**
	 * Called when the game started
	 */
	public void onPlay(boolean starter) {
		lastTime = System.currentTimeMillis();
		isFree = false;
		session.reset();
		if (starter)
			session.onStart();
	}

	/**
	 * Called when city was applied or rejected by the server
	 */
	public void onWordResult(String word, WordResult res) {
		lastTime = System.currentTimeMillis();
		if (res == WordResult.RECEIVED) {
			session.onInputWord(word);
		} else if (res != WordResult.ACCEPTED) {
			log.warn("BOT word was rejected! word={}", word);
		}
	}

	@Override
	public void onDisconnected() {
		super.onDisconnected();
		session.reset();
		isFree = true;
		setOnline(true);
	}

	public void sendCity(String city) {
		onMessage(new LPSWord(city));
	}

	public boolean checkHangingTasks() {
		if (System.currentTimeMillis() - lastTime > Room.MOVE_TIME_MS) {
			log.info("The Bot has found hanging task and disconnect it");
			onDisconnected();
			return true;
		}
		return false;
	}

}

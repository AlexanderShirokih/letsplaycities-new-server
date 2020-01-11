package ru.quandastudio.lpsserver.core;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.AuthType;
import ru.quandastudio.lpsserver.models.LPSMessage;
import ru.quandastudio.lpsserver.models.WordResult;

@Slf4j
@RequiredArgsConstructor
public class Room {

	public static final int MOVE_TIME_MS = 92000;
	private static final int MOVE_TIMER_PERIOD = 5000;

	private final Player starter;
	private final Player invited;

	private ArrayList<String> usedWords = new ArrayList<>();

	private Dictionary dictionary;
	private Player current;
	private char lastChar;
	private int moveCounter = 0;

	public boolean start() {
		final ServerContext context = starter.getCurrentContext();
		dictionary = context.getDictionary();
		current = starter;

		boolean isFriends = context.getFriendshipManager()
				.getFriendsInfo(starter.getUser(), invited.getUser())
				.map((info) -> info.getIsAccepted())
				.orElse(false);
		boolean isBanned = context.getBanlistManager().isBanned(starter.getUser(), invited.getUser());
		sendPlayMessage(starter, true, isFriends, isBanned, invited);
		sendPlayMessage(invited, false, isFriends, isBanned, starter);

		if (isBanned)
			return false;

		startTimer();

		return true;
	}

	// Игрок дал свой ответ
	public void word(Player player, String word) {
		if (player.equals(current)) {
			if (isValid(word)) {
				if (usedWords.contains(word)) {
					submitWord(player, word, WordResult.ALREADY);
				} else if (dictionary.contains(word)) {
					int end = word.length();
					do {
						if (end == 0) {
							log.warn("ERROR!!! end of letters for word={}", word);
							return;
						}
						end--;
						lastChar = word.charAt(end);
					} while (lastChar == 'ь' || lastChar == 'ъ' || lastChar == 'ы' || lastChar == 'ё');

					usedWords.add(word);
					submitWord(player, word, WordResult.ACCEPTED);
					current = oppositePlayer(current);
					submitWord(current, word, WordResult.RECEIVED);
					startTimer();
				} else {
					submitWord(player, word, WordResult.NO_WORD);
				}
			} else {
				submitWord(player, word, WordResult.NO_WORD);
			}
		} else {
			submitWord(player, word, WordResult.WRONG_MOVE);
		}
	}

	private void submitWord(Player player, String word, WordResult result) {
		player.sendMessage(new LPSMessage.LPSWordMessage(result, word));
	}

	private void sendPlayMessage(Player player, boolean isStarter, boolean isFriend, boolean isBanned, Player other) {
		final User oppUser = other.getUser();
		final AuthType authType = other.getAllowSendUID() ? AuthType.from(oppUser.getAuthType()) : null;
		final String snUID = other.getAllowSendUID() ? oppUser.getSnUid() : null;
		final String login = oppUser.getName();
		final String avatar = other.getAvatarData();
		final LPSMessage.LPSPlayMessage play = new LPSMessage.LPSPlayMessage(authType, login, oppUser.getUserId(),
				other.getClientVersion(), other.getClientBuild(), other.getCanReceiveMessages(), isFriend, isStarter,
				isBanned, other.getAllowSendUID());

		if (avatar != null && !avatar.isEmpty()) {
			play.setAvatar(avatar);
		}

		if (other.getAllowSendUID()) {
			play.setSnUID(snUID);
		}

		player.sendMessage(play);
	}

	private void startTimer() {
		moveCounter++;
		GameTimer gt = new GameTimer(this, moveCounter);
		current.getCurrentContext().getTaskLooper().schedule(new DelayedTask(MOVE_TIMER_PERIOD, MOVE_TIMER_PERIOD, gt));
	}

	private boolean isValid(String str) {
		if (str.length() < 2 || (lastChar != 0 && str.charAt(0) != lastChar)) {
			log.warn("ii={} ;lc={}", str, lastChar);
			return false;
		}
		return true;
	}

	public boolean isUsed(String word) {
		return usedWords.contains(word);
	}

	public Player oppositePlayer(Player player) {
		return player.equals(starter) ? invited : starter;
	}

	/**
	 * Битва завершена. Подводим итоги.
	 */
	public void finish() {
		if (usedWords != null) {
			usedWords.clear();
			usedWords = null;
		}
	}

	private void timeOut() {
		finish();

		timeOutPlayer(starter);
		timeOutPlayer(invited);
	}

	private void timeOutPlayer(Player player) {
		if (player.isOnline())
			player.sendMessage(new LPSMessage.LPSTimeoutMessage());
		player.setOnline(false);
		player.setRoom(null);
	}

	class GameTimer implements RunnableTask {
		private Room r;
		private int mID;
		private int time;

		public GameTimer(Room r, int mID) {
			this.r = r;
			this.mID = mID;
			time = 0;
		}

		@Override
		public void run(DelayedTask task) {
			time += MOVE_TIMER_PERIOD;
			if (time >= MOVE_TIME_MS) {
				if (mID == r.moveCounter)
					r.timeOut();
				task.cancel();
			} else {
				if (mID == r.moveCounter) {
					if (!starter.isOnline() || !invited.isOnline())
						task.cancel();
				} else
					task.cancel();
			}
		}
	}

}

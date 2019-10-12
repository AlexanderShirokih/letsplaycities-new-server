package ru.quandastudio.lpsserver.bots;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.Dictionary;
import ru.quandastudio.lpsserver.core.Room;

@Slf4j
@RequiredArgsConstructor
public class BotGameSession {

	private static final float CHANCE_TO_PREFERED = 0.7f;

	private final Bot bot;
	private final byte wordsLevel;
	private final int[] prefCountries;

	private char lastLetter = 0;
	private int move;

	private ScheduledFuture<?> currentTask;

	public void onStart() {
		lastLetter = getRandomFirstChar();
		thinkAboutNextWord();
	}

	public void onInputWord(String word) {
		lastLetter = getLastChar(word);
		thinkAboutNextWord();
	}

	private char getLastChar(String word) {
		char lastChar = 0;
		int end = word.length();
		do {
			if (end == 0) {
				log.warn("ERROR!!! end of letters for word={}", word);
				return 0;
			}
			end--;
			lastChar = word.charAt(end);
		} while (lastChar == 'ь' || lastChar == 'ъ' || lastChar == 'ы' || lastChar == 'ё');
		return lastChar;
	}

	private void thinkAboutNextWord() {
		final int delay = getRandomDelay();
		bot.getCurrentContext().getBotManager().shedule(this, delay, () -> {
			String nextWord = getNextWord();
			if (nextWord != null) {
				bot.sendCity(nextWord);
			}
		});
	}

	private int getLevel() {
		int level;
		move++;

		if (move < 4) {
			level = 0;
		} else if (move < 10) {
			level = 1;
		} else if (move < 20) {
			level = 2;
		} else if (move < 40) {
			level = 3;
		} else
			level = 4;
		return level;
	}

	private int getRandomDelay() {
		int min = bot.getMin();
		int max = bot.getMax();

		final int delay = getLevel() * 1500 + min;
		final int m = Math.max(1, max - min);
		return delay + getRandomInt(min, m);
	}

	private String getNextWord() {
		Room room = bot.getRoom();
		String w = null;

		do {
			w = getRandomWord();
		} while (room != null && w != null && room.isUsed(w));

		return w;
	}

	private String getRandomWord() {
		String word;
		double d = Math.random();
		if (d <= CHANCE_TO_PREFERED && prefCountries.length != 0)
			word = Dictionary.getCity(lastLetter, wordsLevel, prefCountries);
		else
			word = Dictionary.getCity(lastLetter, wordsLevel, null);
		return word;
	}

	private static final char[] FIRST_CHARS = "авгдеиклмнопрст".toCharArray();

	private char getRandomFirstChar() {
		return FIRST_CHARS[getRandomInt(0, FIRST_CHARS.length)];
	}

	private static int getRandomInt(int min, int max) {
		if (max == min)
			max++;
		if (max < min) {
			int t = max;
			max = min;
			min = t;
		}
		return ThreadLocalRandom.current().ints(min, max).findFirst().getAsInt();
	}

	public void reset() {
		move = 0;
		lastLetter = 0;
		if (currentTask != null) {
			currentTask.cancel(false);
		}
	}

	public void setFutureTask(ScheduledFuture<?> futureTask) {
		currentTask = futureTask;
	}

}

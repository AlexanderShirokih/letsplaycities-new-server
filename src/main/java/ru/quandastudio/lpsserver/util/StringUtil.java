package ru.quandastudio.lpsserver.util;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class StringUtil {
	public static String formatName(String name) {
		return name.replaceAll("\"", "").replaceAll("'", "");
	}

	private static final char[] hash_chars = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890!@#$%^&*()_+-="
			.toCharArray();

	public static String getAccIdHash() {
		ThreadLocalRandom tlr = ThreadLocalRandom.current();
		char[] hash = new char[8];
		for (int i = 0; i < hash.length; i++) {
			hash[i] = hash_chars[tlr.nextInt(hash_chars.length)];
		}
		return new String(hash);
	}

	public static String formatDuration(Duration duration) {
		long seconds = duration.getSeconds();
		long absSeconds = Math.abs(seconds);
		String positive = String.format("%d hours, %02d min, %02d sec", absSeconds / 3600, (absSeconds % 3600) / 60,
				absSeconds % 60);
		return seconds < 0 ? "-" + positive : positive;
	}
}

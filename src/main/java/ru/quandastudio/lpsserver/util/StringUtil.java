package ru.quandastudio.lpsserver.util;

import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class StringUtil {
	public static String formatName(String name) {
		return name.replaceAll("\"", "").replaceAll("\'", "");
	}

	public static String max(String str, int max) {
		if (str.length() > max)
			return str.substring(0, max - 1);
		return str;
	}

	public static String range(String str, int min, int max) {
		if (str.length() < min) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < min - str.length(); i++)
				sb.append('#');
			str += sb;
		} else
			str = max(str, max);
		return str;
	}

	private static final char[] hash_chars = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890!@#$%^&*()_+-="
			.toCharArray();

	public static final String getAccIdHash() {
		ThreadLocalRandom tlr = ThreadLocalRandom.current();
		char[] hash = new char[8];
		for (int i = 0; i < hash.length; i++) {
			hash[i] = hash_chars[tlr.nextInt(hash_chars.length)];
		}
		return new String(hash);
	}

	public static String toBase64(byte[] data) {
		return data == null ? null : Base64.getEncoder().encodeToString(data);
	}

	public static String formatDuration(Duration duration) {
		long seconds = duration.getSeconds();
		long absSeconds = Math.abs(seconds);
		String positive = String.format("%d hours, %02d min, %02d sec", absSeconds / 3600, (absSeconds % 3600) / 60,
				absSeconds % 60);
		return seconds < 0 ? "-" + positive : positive;
	}
}

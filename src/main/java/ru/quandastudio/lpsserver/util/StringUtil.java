package ru.quandastudio.lpsserver.util;

import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class StringUtil {
	public static String formatName(String name) {
		return name.replaceAll("\"", "").replaceAll("\'", "");
	}

	public static String max(String str, int max) {
		if (str.length() > max)
			str = str.substring(0, max - 1);
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
		return Base64.getEncoder().encodeToString(data);
	}
}

package ru.quandastudio.lpsserver.bots;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;

import ru.quandastudio.lpsserver.LPSException;

public class BotConfigParser {

	public BotInfo[] parseBotsList(File file) throws IOException {
		return parseBotsList(new String(Files.readAllBytes(file.toPath())));
	}

	public BotInfo[] parseBotsList(String source) {
		JSONObject json = new JSONObject(source);
		JSONArray arrayOfBots = json.getJSONArray("bots");

		int len = arrayOfBots.length();
		BotInfo[] bots = new BotInfo[len];

		for (int i = 0; i < len; i++) {
			bots[i] = parseJsonBotObject(arrayOfBots.getJSONObject(i));
		}
		return bots;
	}

	private BotInfo parseJsonBotObject(JSONObject obj) {
		BotInfo bot = new BotInfo();
		bot.name = obj.getString("name");
		bot.wordsLevel = obj.getInt("words_level");
		bot.user_id = obj.getInt("user_id");
		bot.avatar = parseAvatar(obj.getString("avatar"));
		bot.prefCountries = parsePrefCountries(obj.getJSONArray("pref_country"));
		bot.minWaitingTimeMs = obj.optInt("min", 5) * 1000;
		bot.maxWaitingTimeMs = obj.optInt("max", 20) * 1000;
		return bot;
	}

	private int[] parsePrefCountries(JSONArray pref) {
		int len = pref.length();
		int[] prefVal = new int[len];
		for (int i = 0; i < len; i++) {
			prefVal[i] = findCountryCode(pref.getString(i));
		}
		return prefVal;
	}

	private byte[] parseAvatar(String avatar) {
		byte[] data = null;
		if (avatar != null) {
			try {
				// Important! Image size should be equal to 128 in both dimensions
				data = Files.readAllBytes(new ClassPathResource(avatar).getFile().toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return data;
	}

	private int findCountryCode(String code) {
		switch (code) {
		case "ru":
			return 51;
		case "ua":
			return 196;
		case "by":
			return 96;
		case "kz":
			return 18;
		case "us":
			return 44;
		}
		throw new LPSException("Unknown country code: " + code);
	}
}

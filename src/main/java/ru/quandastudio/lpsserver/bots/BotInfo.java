package ru.quandastudio.lpsserver.bots;

import lombok.Data;

@Data
public class BotInfo {
	public String name;
	public byte[] avatar;
	public int wordsLevel;
	public int user_id;
	public int[] prefCountries;
	public int minWaitingTimeMs;
	public int maxWaitingTimeMs;
}

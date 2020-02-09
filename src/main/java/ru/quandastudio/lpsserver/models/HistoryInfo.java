package ru.quandastudio.lpsserver.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@NonNull
@EqualsAndHashCode
public class HistoryInfo {
	final int userId;
	final String login;
	@Setter
	boolean isFriend;
	final long startTime;
	final int duration;
	final int wordsCount;
	@Deprecated
	final String pictureHash;
}

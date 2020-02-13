package ru.quandastudio.lpsserver.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.data.entities.HistoryProjection;

@RequiredArgsConstructor
@Getter
@NonNull
@EqualsAndHashCode
public class HistoryInfo {
	final int userId;
	final String login;
	final boolean isFriend;
	final long startTime;
	final int duration;
	final int wordsCount;
	@Deprecated
	final String pictureHash;

	public HistoryInfo(HistoryProjection h) {
		this(h.getUserId(), h.getLogin(), h.getIsFriend(), h.getCreationDate().getTime(), h.getDuration(),
				h.getWordsCount(), "");
	}
}

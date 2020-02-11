package ru.quandastudio.lpsserver.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.quandastudio.lpsserver.data.entities.HistoryItem;
import ru.quandastudio.lpsserver.data.entities.User;

@RequiredArgsConstructor
@Getter
@NonNull
@EqualsAndHashCode
public class HistoryInfo {
	final transient User user;

	final int userId;
	final String login;
	@Setter
	boolean isFriend;
	final long startTime;
	final int duration;
	final int wordsCount;
	@Deprecated
	final String pictureHash;

	public HistoryInfo(User other, HistoryItem item) {
		this(other, other.getUserId(), other.getName(), item.getCreationDate().getTime(), item.getDuration(),
				item.getWordsCount(), "");
	}
}

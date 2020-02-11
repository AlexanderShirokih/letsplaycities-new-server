package ru.quandastudio.lpsserver.models;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.data.entities.BannedUser;

@RequiredArgsConstructor
@Getter
@NonNull
public class BlackListItem {
	private final String login;
	private final int userId;

	public BlackListItem(BannedUser user) {
		this(user.getBannedName(), user.getBannedId());
	}
}
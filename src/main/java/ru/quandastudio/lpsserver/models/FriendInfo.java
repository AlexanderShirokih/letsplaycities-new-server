package ru.quandastudio.lpsserver.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.data.entities.FriendshipProjection;

@RequiredArgsConstructor
@Getter
@NonNull
@EqualsAndHashCode
public class FriendInfo {
	private final int userId;
	private final String login;
	private final boolean accepted;
	private final String pictureHash;

	public FriendInfo(FriendshipProjection f) {
		this(f.getUserId(), f.getLogin(), f.getAccepted(), f.getPictureHash());
	}
}

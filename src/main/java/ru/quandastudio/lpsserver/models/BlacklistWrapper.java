package ru.quandastudio.lpsserver.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.data.entities.OppUserNameProjection;

@RequiredArgsConstructor
@Getter
@NonNull
@EqualsAndHashCode
public class BlacklistWrapper {

	private final int userId;

	private final String login;
	
	private final String pictureHash;

	public BlacklistWrapper(OppUserNameProjection user) {
		this(user.getUserId(), user.getLogin(), user.getPictureHash());
	}
}

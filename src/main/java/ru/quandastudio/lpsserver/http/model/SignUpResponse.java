package ru.quandastudio.lpsserver.http.model;

import lombok.Getter;
import lombok.NonNull;
import ru.quandastudio.lpsserver.data.entities.AuthData;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.data.entities.User.State;
import ru.quandastudio.lpsserver.models.AuthType;

@Getter
public class SignUpResponse {

	@NonNull
	private final Integer userId;

	@NonNull
	private final String accHash;

	@NonNull
	private final String name;

	@NonNull
	private final State state;

	@NonNull
	private final AuthType authType;

	private final String picHash;

	public SignUpResponse(final AuthData authData) {
		User user = authData.getUser();
		userId = user.getUserId();
		picHash = user.getAvatarHash();
		state = user.getState();
		name = user.getName();
		accHash = authData.getAccessHash();
		authType = AuthType.from(authData.getAuthType());
	}

}

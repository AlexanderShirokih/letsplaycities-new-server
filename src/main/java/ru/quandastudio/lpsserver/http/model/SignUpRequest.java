package ru.quandastudio.lpsserver.http.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.Setter;
import ru.quandastudio.lpsserver.models.AuthType;
import ru.quandastudio.lpsserver.validation.CheckInBanlist;

@Data
public class SignUpRequest {
	@NotNull
	@Min(value = 5, message = "Unsupported protocol version")
	@Max(value = 5, message = "Unsupported protocol version")
	private int version;

	@NotNull
	@Size(min = 2, max = 64, message = "Login must be between 2 and 64 characters")
	@CheckInBanlist
	@Setter
	private String login;

	@NotNull
	private AuthType authType;

	@Size(max = 200)
	private String firebaseToken;

	@NotNull
	private String accToken;

	@NotNull
	@Size(min = 2, max = 32, message = "Social network UID must be in size [2;32]!")
	@Setter
	private String snUID;
}

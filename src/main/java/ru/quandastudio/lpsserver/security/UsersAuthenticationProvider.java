package ru.quandastudio.lpsserver.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.User;

@Component
public class UsersAuthenticationProvider implements AuthenticationProvider {

	@Autowired
	private ServerContext context;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		final Integer userId = Result.success(authentication.getName())
				.map(Integer::parseInt)
				.mapError((Exception e) -> new BadCredentialsException("Invalid userId", e))
				.get();
		final String accessHash = authentication.getCredentials().toString();

		return context.getUserManager()
				.getUserByIdAndHash(userId, accessHash)
				.mapError((Exception e) -> new BadCredentialsException(e.getMessage()))
				.map((User u) -> new UserAuthenticationToken(u))
				.get();
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}

}

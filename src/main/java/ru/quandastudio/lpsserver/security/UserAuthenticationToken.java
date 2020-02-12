package ru.quandastudio.lpsserver.security;

import java.util.Collections;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.Getter;
import ru.quandastudio.lpsserver.data.entities.User;

@Getter
public class UserAuthenticationToken extends AbstractAuthenticationToken {

	private static final long serialVersionUID = 1L;

	private final User user;

	public UserAuthenticationToken(User user) {
		super(Collections.singletonList(new SimpleGrantedAuthority("USER")));
		this.user = user;
		super.setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return null;
	}

	@Override
	public Object getPrincipal() {
		return user;
	}
}

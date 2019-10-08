package ru.quandastudio.lpsserver.netty.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AuthType {

	Native("nv"), Google("gl"), Vkontakte("vk"), Odnoklassniki("ok"), Facebook("fb");

	@Getter
	final String name;

	public static AuthType from(String t) {
		for (AuthType type : AuthType.values())
			if (type.name.equals(t))
				return type;
		throw new IllegalArgumentException("Unknown AuthType " + t);
	}
}

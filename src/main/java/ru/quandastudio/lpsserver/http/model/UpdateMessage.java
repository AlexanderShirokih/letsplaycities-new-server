package ru.quandastudio.lpsserver.http.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class UpdateMessage {

	private final Dictionary dictionary;

	@RequiredArgsConstructor
	@Getter
	public static class Dictionary {
		private final Integer version;
	}

}

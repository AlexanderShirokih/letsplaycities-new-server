package ru.quandastudio.lpsserver.http.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class UpdateMessage {

	@RequiredArgsConstructor
	@Getter
	public static class Dictionary {
		private final Integer version;
	}
	
	private final Dictionary dictionary;
}

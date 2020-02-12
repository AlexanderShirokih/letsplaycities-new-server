package ru.quandastudio.lpsserver.http.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MessageWrapper<T> {

	private final T data;

	private final String error;

	public static <T> MessageWrapper<T> of(@NonNull T data) {
		return new MessageWrapper<T>(data, null);
	}
}

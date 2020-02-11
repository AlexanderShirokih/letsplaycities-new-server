package ru.quandastudio.lpsserver.http.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MessageWrapper<T> {

	private final String error;

	private final T data;
}

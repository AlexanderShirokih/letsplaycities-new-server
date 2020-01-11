package ru.quandastudio.lpsserver.models;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@NonNull
public class BlackListItem {
	private final String login;
	private final int userId;
}
package ru.quandastudio.lpsserver.netty.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@NonNull
@EqualsAndHashCode
public class FriendInfo {
	private final int userId;
	private final String login;
	private final boolean accepted;
}

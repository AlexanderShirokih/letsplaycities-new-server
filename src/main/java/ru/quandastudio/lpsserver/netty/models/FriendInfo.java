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
	final int userId;
	final String login;
	final boolean accepted;
}

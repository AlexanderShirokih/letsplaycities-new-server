package ru.quandastudio.lpsserver.data.entities;

import org.springframework.beans.factory.annotation.Value;

public interface FriendshipProjection {

	@Value("#{target.oppUser.userId}")
	Integer getUserId();

	@Value("#{target.oppUser.name}")
	String getLogin();

	boolean getAccepted();

}

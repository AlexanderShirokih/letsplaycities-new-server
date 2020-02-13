package ru.quandastudio.lpsserver.data.entities;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Value;

public interface HistoryProjection {

	@Value("#{target.opp_id.userId}")
	Integer getUserId();

	@Value("#{target.opp_id.name}")
	String getLogin();

	boolean getIsFriend();

	Timestamp getCreationDate();

	Integer getDuration();

	Integer getWordsCount();
}

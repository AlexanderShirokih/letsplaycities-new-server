package ru.quandastudio.lpsserver.data.entities;

import java.sql.Timestamp;

public interface HistoryProjection extends OppUserNameProjection {

	boolean getIsFriend();

	Timestamp getCreationDate();

	Integer getDuration();

	Integer getWordsCount();
}

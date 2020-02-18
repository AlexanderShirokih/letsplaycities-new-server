package ru.quandastudio.lpsserver.models;

public enum RequestType {
	@Deprecated
	QUERY_LIST, @Deprecated
	DELETE,
	/** Create new request **/
	SEND,
	/** Accept created request */
	ACCEPT,
	/** Decline created request */
	DENY;
}
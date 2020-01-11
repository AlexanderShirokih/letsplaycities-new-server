package ru.quandastudio.lpsserver.models;

public enum WordResult {
	RECEIVED, ACCEPTED, ALREADY, NO_WORD, WRONG_MOVE;

	static WordResult from(int index) {
		return WordResult.values()[index];
	}
}

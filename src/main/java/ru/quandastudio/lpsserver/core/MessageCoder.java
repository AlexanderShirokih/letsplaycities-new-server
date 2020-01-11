package ru.quandastudio.lpsserver.core;

import ru.quandastudio.lpsserver.models.LPSClientMessage;
import ru.quandastudio.lpsserver.models.LPSMessage;

public interface MessageCoder {

	/**
	 * Converts LPSMessage to string representation
	 * 
	 * @param message message to encode
	 * @return Encoded message
	 */
	String encode(LPSMessage message);

	/**
	 * Converts string representation to LPSClientMessage
	 * 
	 * @param message message to decode
	 * @return Decoded message
	 */
	LPSClientMessage decode(String message);

}

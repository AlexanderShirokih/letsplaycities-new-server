package ru.quandastudio.lpsserver.core;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import ru.quandastudio.lpsserver.netty.models.LPSClientMessage;
import ru.quandastudio.lpsserver.netty.models.LPSMessage;

@AllArgsConstructor
@Component
public class JsonMessageCoder implements MessageCoder {

	private final Gson gson;

	@Override
	public String encode(LPSMessage message) {
		return gson.toJson(message);
	}

	@Override
	public LPSClientMessage decode(String message) {
		return gson.fromJson(message, LPSClientMessage.class);
	}

}

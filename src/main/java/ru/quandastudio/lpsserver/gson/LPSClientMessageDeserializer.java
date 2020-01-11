package ru.quandastudio.lpsserver.gson;

import java.lang.reflect.Type;
import java.util.Arrays;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import ru.quandastudio.lpsserver.models.Action;
import ru.quandastudio.lpsserver.models.LPSClientMessage;

public class LPSClientMessageDeserializer implements JsonDeserializer<LPSClientMessage> {

	private Class<?>[] lpsMessages = LPSClientMessage.class.getDeclaredClasses();

	@Override
	public LPSClientMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		JsonObject jsonObj = json.getAsJsonObject();
		final String action = jsonObj.has("action") ? jsonObj.get("action").getAsString() : null;

		return Arrays.stream(lpsMessages).filter((Class<?> clz) -> {
			final Action a = clz.getAnnotation(Action.class);
			return LPSClientMessage.class.isAssignableFrom(clz) && a != null && a.value().equals(action);
		}).map((Class<?> type) -> {
			return (LPSClientMessage) context.deserialize(json, type);
		}).findFirst().orElse(new LPSClientMessage.LPSLeave("Deserialization error!"));
	}

}

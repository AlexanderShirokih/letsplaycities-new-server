package ru.quandastudio.lpsserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ru.quandastudio.lpsserver.gson.LPSClientMessageDeserializer;
import ru.quandastudio.lpsserver.models.LPSClientMessage;

@Configuration
public class GsonConfigutation {

	@Bean
	@Scope("singleton")
	public Gson gson() {
		return new GsonBuilder().registerTypeAdapter(LPSClientMessage.class, new LPSClientMessageDeserializer())
				.create();
	}
}

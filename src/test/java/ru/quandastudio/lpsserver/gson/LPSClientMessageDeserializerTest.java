package ru.quandastudio.lpsserver.gson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ru.quandastudio.lpsserver.models.LPSClientMessage;

public class LPSClientMessageDeserializerTest {

	private Gson gson;

	@Before
	public void setUp() {
		gson = new GsonBuilder().registerTypeAdapter(LPSClientMessage.class, new LPSClientMessageDeserializer())
								.create();
	}

	@Test
	public void testDeserializeMessage() {
		String msg = "{\"word\":\"test\",\"action\":\"word\"}";

		LPSClientMessage output = gson.fromJson(msg, LPSClientMessage.class);

		assertTrue(output instanceof LPSClientMessage.LPSWord);
		assertEquals("word", output.getAction());
	}

}

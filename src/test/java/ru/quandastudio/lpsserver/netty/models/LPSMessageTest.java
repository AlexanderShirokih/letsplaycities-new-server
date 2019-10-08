package ru.quandastudio.lpsserver.netty.models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.Gson;

import ru.quandastudio.lpsserver.netty.models.LPSMessage.LPSFriendsList;

public class LPSMessageTest {

	@Test
	public void testFriendsListDeserialization() {
		String input = "{\"data\":[{\"accepted\":true,\"login\":\"Test\",\"userId\":17873},"
				+ "{\"accepted\":false,\"login\":\"Superheroes\",\"userId\":178},"
				+ "{\"accepted\":true,\"login\":\"Unit\",\"userId\":9611}]}";

		LPSFriendsList output = new Gson().fromJson(input, LPSMessage.LPSFriendsList.class);

		assertEquals(output.getData().size(), 3);
		assertEquals(output.getData().get(0), new FriendInfo(17873, "Test", true));
	}

}

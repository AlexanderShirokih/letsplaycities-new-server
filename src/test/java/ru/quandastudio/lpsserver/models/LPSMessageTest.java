package ru.quandastudio.lpsserver.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import ru.quandastudio.lpsserver.models.LPSMessage.LPSFriendsList;

public class LPSMessageTest {

    @Test
    public void testFriendsListDeserialization() {
        String input = "{\"data\":[{\"accepted\":true,\"login\":\"Test\",\"userId\":17873},"
                + "{\"accepted\":false,\"login\":\"Superheroes\",\"userId\":178},"
                + "{\"accepted\":true,\"isSender\":false,\"login\":\"Unit\",\"userId\":9611}]}";

        LPSFriendsList output = new Gson().fromJson(input, LPSMessage.LPSFriendsList.class);

        assertEquals(output.getData().size(), 3);
        assertEquals(output.getData().get(0), new FriendInfo(17873, "Test", true, false, null));
    }

}

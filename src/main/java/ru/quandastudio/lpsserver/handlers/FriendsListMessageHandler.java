package ru.quandastudio.lpsserver.handlers;

import ru.quandastudio.lpsserver.core.Player;
import ru.quandastudio.lpsserver.models.FriendInfo;
import ru.quandastudio.lpsserver.models.LPSClientMessage.LPSFriendList;
import ru.quandastudio.lpsserver.models.LPSMessage.LPSFriendsList;

import java.util.List;

@Deprecated(forRemoval = true, since = "1.4.1")
public class FriendsListMessageHandler extends MessageHandler<LPSFriendList> {

    public FriendsListMessageHandler() {
        super(LPSFriendList.class);
    }

    @Override
    public void handle(Player player, LPSFriendList msg) {
        final List<FriendInfo> friendInfoList = player
                .getCurrentContext()
                .getFriendshipManager()
                .getFriendsList(player.getUser());
        player.sendMessage(new LPSFriendsList(friendInfoList));
    }

}

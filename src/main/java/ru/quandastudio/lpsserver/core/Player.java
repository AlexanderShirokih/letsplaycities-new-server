package ru.quandastudio.lpsserver.core;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.LPSClientMessage;
import ru.quandastudio.lpsserver.models.LPSMessage;
import ru.quandastudio.lpsserver.models.LPSMessage.LPSLeaveMessage;
import ru.quandastudio.lpsserver.models.Role;

import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@RequiredArgsConstructor
public class Player {

    @NonNull
    private final ServerContext currentContext;

    @NonNull
    private final MessageChannel channel;

    private User user;

    private Boolean canReceiveMessages;

    private String clientVersion;

    private Integer clientBuild;

    private transient boolean isOnline = true;

    private Room room;

    public void sendMessage(LPSMessage msg) {
        channel.send(msg);
    }

    public void onMessage(LPSClientMessage msg) {
        currentContext.getMessageRouter().handleMessage(this, msg);
    }

    public void onDisconnected() {
        setOnline(false);
        if (room != null) {
            Player p = room.oppositePlayer(this);
            if (p.isOnline())
                p.sendMessage(new LPSLeaveMessage(false, getUser().getId()));
            else
                room.finish();
        }
        getCurrentContext().getFriendsRequests().remove(this);
    }

    public boolean isAtLeastHasVersion(int version) {
        return clientBuild >= version;
    }

    public boolean hasAdminPrivileges() {
        return user.getRole().isAtLeast(Role.ADMIN);
    }

    public boolean isAuthorized() {
        return user != null && user.getRole().isAtLeast(Role.REGULAR_USER);
    }

    public boolean isFriend(Integer userId) {
        return getCurrentContext().getFriendshipManager().isFriends(getUser(), new User(userId));
    }

    public Optional<Player> getOppositePlayer() {
        return room != null ? Optional.of(room.oppositePlayer(this)) : Optional.empty();
    }

    @Override
    public int hashCode() {
        return Objects.hash(user);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Player other = (Player) obj;
        return Objects.equals(user, other.user);
    }

    public static Player createDummyPlayer(ServerContext context, Integer userId) {
        Player p = new Player(context, new MessageChannel() {
        });
        p.setUser(new User(userId));
        p.setOnline(false);
        return p;
    }

}

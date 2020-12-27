package ru.quandastudio.lpsserver.data.entities;

import ru.quandastudio.lpsserver.models.BanStatus;
import ru.quandastudio.lpsserver.models.FriendshipStatus;

/**
 * Projection interface used to show user profile main info
 * from another user perspective
 */
public interface SpectatedProfileView extends ProfileView {

    /**
     * If friendship exists it will contain request sender id
     */
    FriendshipStatus getFriendshipType();

    /**
     * If friendship exists it will contain acceptance status
     */
    BanStatus getBanType();
}

package ru.quandastudio.lpsserver.models;

/**
 * Friendship status between two users
 */
public enum FriendshipStatus {
    /**
     * Users aren't friends
     */
    notFriends,

    /**
     * Users are friends
     */
    friends,

    /**
     * There is a request from owner to target
     */
    inputRequest,

    /**
     * There is a request from target to owner
     */
    outputRequest,

    /**
     * There is owner profile (no target)
     */
    owner
}

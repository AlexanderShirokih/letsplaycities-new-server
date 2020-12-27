package ru.quandastudio.lpsserver.models;

/**
 * Ban status between two users
 */
public enum BanStatus {
    /**
     * Neither user or owner bans each other
     */
    notBanned,

    /**
     * Owner was banned by user
     */
    inputBan,

    /**
     * This user banned by owner
     */
    outputBan,

    /**
     * There is owner profile
     */
    owner
}

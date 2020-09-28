package ru.quandastudio.lpsserver.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Describes user role on server
 */
@RequiredArgsConstructor
@Getter
public enum Role {
    BANNED_USER("banned", "banned"), REGULAR_USER("ready", "regular"), ADMIN("admin", "admin");

    final String legacyName;
    final String modernName;

    /**
     * Checks that entity is at least in {@code state}
     *
     * @param role required state
     * @return {@literal true} if entity is at least in {@code state},
     * {@literal false} otherwise
     */
    public boolean isAtLeast(Role role) {
        return ordinal() >= role.ordinal();
    }
}
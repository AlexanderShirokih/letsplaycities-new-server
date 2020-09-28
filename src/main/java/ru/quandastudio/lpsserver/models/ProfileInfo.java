package ru.quandastudio.lpsserver.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.data.entities.ProfileView;

/**
 * Data class that describes user profile info
 */
@RequiredArgsConstructor
@Getter
@NonNull
@EqualsAndHashCode
public class ProfileInfo {

    private final int userId;
    private final String login;
    private final long lastVisitDate;
    private final Role role;
    private final String pictureHash;


    /**
     * Creates ProfileInfo from ProfileView
     */
    public ProfileInfo(ProfileView p) {
        this(p.getId(), p.getName(), p.getLastVisitDate().toInstant().toEpochMilli(), p.getRole(), p.getAvatarHash());
    }
}



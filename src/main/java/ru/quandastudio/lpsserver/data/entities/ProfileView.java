package ru.quandastudio.lpsserver.data.entities;

import ru.quandastudio.lpsserver.models.Role;

import java.sql.Timestamp;

/**
 * Projection interface used to show user profile main info
 */
public interface ProfileView {

    int getId();

    String getName();

    Timestamp getLastVisitDate();

    String getAvatarHash();

    Role getRole();
}

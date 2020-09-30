package ru.quandastudio.lpsserver.data.entities;

import ru.quandastudio.lpsserver.models.Role;

import java.sql.Timestamp;

/**
 * Projection interface used to show user profile main info
 */
public interface ProfileView {

    /**
     * User owner id
     */
    int getId();

    /**
     * User name
     */
    String getName();

    /**
     * Users last visit date
     */
    Timestamp getLastVisitDate();

    /**
     * Users avatar path
     */
    String getAvatarHash();

    /**
     * Users role on the server
     */
    Role getRole();
}

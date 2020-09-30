package ru.quandastudio.lpsserver.data.entities;

/**
 * Projection interface used to show user profile main info
 * from another user perspective
 */
public interface SpectatedProfileView extends ProfileView {

    /**
     * If friendship exists it will contain request sender id
     */
    Boolean getIsSender();

    /**
     * If friendship exists it will contain acceptance status
     */
    Boolean getIsAccepted();
}

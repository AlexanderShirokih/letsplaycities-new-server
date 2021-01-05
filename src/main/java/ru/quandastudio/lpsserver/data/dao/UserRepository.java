package ru.quandastudio.lpsserver.data.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.quandastudio.lpsserver.data.entities.ProfileView;
import ru.quandastudio.lpsserver.data.entities.SpectatedProfileView;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.AuthType;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByIdAndAccessHash(Integer userId, String accessHash);

    Optional<User> findBySnUidAndAuthType(String snUid, AuthType authType);

    /**
     * Finds profile view of user with given userId
     *
     * @param userId target user id
     * @return profile view if user exists
     */
    Optional<ProfileView> findProfileViewById(Integer userId);

    /**
     * Finds profile view from another origin
     *
     * @param user      target user id
     * @param spectator origin user
     * @return profile view if user exists
     */
    @Query(value = "SELECT u.id          as id,\n" +
            "u.name        as name,\n" +
            "u.lastVisitDate  as lastVisitDate,\n" +
            "u.avatarHash as avatarHash,\n" +
            "u.role        as role,\n" +
            "u.authType as authType,\n" +
            "case when f.isAccepted is null then 0\n" +
            " when f.isAccepted = true then 1\n" +
            " when u = f.receiver then 2\n" +
            " else 3 end as friendshipType,\n" +
            "case when b.baner is null then 0\n" +
            " when u = b.baner then 1\n" +
            " else 2 end as banType\n" +
            "FROM User u\n" +
            "         LEFT JOIN Friendship f\n" +
            "                   ON\n" +
            "                           (u = f.sender and f.receiver = ?2)\n" +
            "                           OR (u = f.receiver and f.sender = ?2)\n" +
            "         LEFT JOIN Banlist b\n" +
            "                   ON\n" +
            "                           (u = b.baner and b.banned = ?2)\n" +
            "                           OR (u = b.banned and b.baner = ?2)\n" +
            "WHERE u = ?1")
    Optional<SpectatedProfileView> findProfileViewFromSpectator(User user, User spectator);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.firebaseToken = ?2 WHERE id = ?1")
    void updateFirebaseToken(Integer id, String token);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.avatarHash = ?2 WHERE u = ?1")
    void updateAvatarHash(User user, String hash);


}

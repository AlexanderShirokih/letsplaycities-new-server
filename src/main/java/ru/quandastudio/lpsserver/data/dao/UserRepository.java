package ru.quandastudio.lpsserver.data.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.AuthType;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

	Optional<User> findByIdAndAccessHash(Integer userId, String accessHash);

	Optional<User> findBySnUidAndAuthType(String snUid, AuthType authType);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE User u SET u.firebaseToken = ?2 WHERE id = ?1")
	void updateFirebaseToken(Integer id, String token);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE User u SET u.avatarHash = ?2 WHERE u = ?1")
	void updateAvatarHash(User user, String hash);
	
}

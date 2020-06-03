package ru.quandastudio.lpsserver.data.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import ru.quandastudio.lpsserver.data.entities.AuthData;
import ru.quandastudio.lpsserver.data.entities.User;

public interface AuthDataRepository extends JpaRepository<AuthData, Integer> {

	public Optional<AuthData> findByUser(Integer userId);
	
	public Optional<AuthData> findByUserAndAccessHash(Integer userId, String accessId);

	public Optional<AuthData> findBySnUidAndAuthType(String snUid, String authType);

	
	@Modifying(clearAutomatically = true)
	@Query("UPDATE AuthData a SET a.firebaseToken = ?2 WHERE user = ?1")
	public void updateFirebaseToken(User user, String token);

	public String findFirebaseTokenByUser(User user);

}

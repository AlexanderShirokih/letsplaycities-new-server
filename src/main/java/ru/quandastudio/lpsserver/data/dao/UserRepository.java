package ru.quandastudio.lpsserver.data.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

	@SuppressWarnings("unchecked")
	public User save(User user);

	public Optional<User> findByUserIdAndAccessId(Integer userId, String accessId);

	public Optional<User> findBySnUidAndAuthType(String snUid, String authType);

}

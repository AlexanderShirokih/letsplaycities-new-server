package ru.quandastudio.lpsserver.data.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.FriendshipProjection;
import ru.quandastudio.lpsserver.data.entities.User;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

	@SuppressWarnings("unchecked")
	public Friendship save(Friendship friendship);

	@Modifying
	@Query("delete from Friendship f where (f.sender = ?1 and f.receiver = ?2) or (f.sender = ?2 and f.receiver = ?1)")
	public void deleteBySenderAndReceiverOrReceiverAndSender(User sender, User receiver);

	@Query("select f from Friendship f where (f.sender = ?1 and f.receiver = ?2) or (f.sender = ?2 and f.receiver = ?1)")
	public Optional<Friendship> findBySenderAndReceiverOrReceiverAndSender(User sender, User receiver);

	@Query("select f from Friendship f where f.isAccepted = true and (f.sender = :user and f.receiver in (:other)) or (f.receiver = :user and f.sender in (:other))")
	public List<Friendship> findBySenderAndReceiverIn(@Param("user") User user, @Param("other") List<User> other);

	@Query("select case when f.sender=?1 then f.receiver else f.sender end as oppUser, f.isAccepted as accepted from Friendship f where f.sender = ?1 or f.receiver = ?1")
	public List<FriendshipProjection> findAllFriendsByUser(User user);

	@Modifying(clearAutomatically = true)
	@Query("update Friendship f set f.sender = ?1, f.receiver = ?2 where f.sender = ?2 and f.receiver = ?1")
	public void swapSenderAndReceiver(User newSender, User newReceiver);

	public Optional<Friendship> findBySenderAndReceiver(User sender, User receiver);
}

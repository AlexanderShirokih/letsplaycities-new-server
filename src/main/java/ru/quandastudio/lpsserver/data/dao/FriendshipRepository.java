package ru.quandastudio.lpsserver.data.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.FriendshipProjection;
import ru.quandastudio.lpsserver.data.entities.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

	@Modifying
	@Query("delete from Friendship f where (f.sender = ?1 and f.receiver = ?2) or (f.sender = ?2 and f.receiver = ?1)")
	void deleteBySenderAndReceiverOrReceiverAndSender(User sender, User receiver);

	@Query("select f from Friendship f where (f.sender = ?1 and f.receiver = ?2) or (f.sender = ?2 and f.receiver = ?1)")
	Optional<Friendship> findBySenderAndReceiverOrReceiverAndSender(User sender, User receiver);

	@Query("select case when f.sender=?1 then f.receiver else f.sender end as oppUser, f.isAccepted as accepted from Friendship f where f.sender = ?1 or f.receiver = ?1")
	List<FriendshipProjection> findAllFriendsByUser(User user);

	@Modifying(clearAutomatically = true)
	@Query("update Friendship f set f.sender = ?1, f.receiver = ?2 where f.sender = ?2 and f.receiver = ?1")
	void swapSenderAndReceiver(User newSender, User newReceiver);

	Optional<Friendship> findBySenderAndReceiverAndIsAcceptedFalse(User sender, User receiver);
}

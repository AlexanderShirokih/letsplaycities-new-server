package ru.quandastudio.lpsserver.data.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.BannedUser;

@Repository
public interface BannedUserRepository extends JpaRepository<BannedUser, Long> {

	@SuppressWarnings("unchecked")
	public BannedUser save(BannedUser user);

	public void deleteByBanerIdAndBannedId(Integer banerId, Integer bannedId);

	@Query("select b from BannedUser b where b.banerId = ?1 or b.bannedId = ?1")
	public List<BannedUser> findByBanerIdOrBannedId(Integer banerId);

	@Query("select case when count(b)> 0 then true else false end from BannedUser b where (b.banerId=?1 and b.bannedId=?2) or (b.banerId=?2 and b.bannedId=?1)")
	public boolean existsByUserId(Integer firstUserId, Integer secondUserId);

}

package ru.quandastudio.lpsserver.data.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.BlackListUser;
import ru.quandastudio.lpsserver.data.entities.OppUserNameProjection;
import ru.quandastudio.lpsserver.data.entities.User;

@Repository
public interface BlacklistRepository extends JpaRepository<BlackListUser, Long> {

	@SuppressWarnings("unchecked")
	public BlackListUser save(BlackListUser user);

	public void deleteByBanerAndBanned(User baner, User banned);

	@Query("select b.banned as oppUser from BlackListUser b where b.baner = ?1")
	public List<OppUserNameProjection> findBannedUsersByUser(User user);

	@Query("select case when count(b)> 0 then true else false end from BlackListUser b where (b.baner=?1 and b.banned=?2) or (b.baner=?2 and b.banned=?1)")
	public boolean existsByUser(User firstUser, User secondUser);

}

package ru.quandastudio.lpsserver.data.dao;

import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.HistoryProjection;
import ru.quandastudio.lpsserver.data.entities.HistoryItem;
import ru.quandastudio.lpsserver.data.entities.User;

@Repository
public interface HistoryRepository extends JpaRepository<HistoryItem, Long> {

	@SuppressWarnings("unchecked")
	public HistoryItem save(HistoryItem item);

	@Query(value="SELECT \n" + 
			"   CASE WHEN h.starter=?1 THEN h.invited ELSE h.starter END AS opp_id,\n" + 
			"   CASE WHEN f.isAccepted IS NOT null THEN f.isAccepted else false END AS isFriend,\n" + 
			"	h.creationDate AS creationDate,\n" +
			"	h.duration AS duration,\n"+
			"	h.wordsCount AS wordsCount\n" +
			"FROM \n" + 
			"    HistoryItem h\n" + 
			"LEFT JOIN Friendship f\n" + 
			"ON (f.sender=h.starter AND f.receiver = h.invited) OR (f.sender=h.invited AND f.receiver = h.starter)\n" + 
			"WHERE \n" + 
			"    h.starter = ?1 OR h.invited=?1\n" + 
			"ORDER BY h.id DESC")	 
	public Stream<HistoryProjection> findHistoryJoiningFriends(User user);

}

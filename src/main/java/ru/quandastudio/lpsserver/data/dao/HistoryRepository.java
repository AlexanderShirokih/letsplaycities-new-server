package ru.quandastudio.lpsserver.data.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.quandastudio.lpsserver.data.entities.History;
import ru.quandastudio.lpsserver.data.entities.HistoryProjection;
import ru.quandastudio.lpsserver.data.entities.User;

import java.util.stream.Stream;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    @Query(value = "SELECT \n" +
            "   CASE WHEN h.starter=?1 THEN h.invited ELSE h.starter END AS oppUser,\n" +
            "   COALESCE(f.isAccepted, false) AS isFriend,\n" +
            "	h.creationDate AS creationDate,\n" +
            "	h.duration AS duration,\n" +
            "	h.wordsCount AS wordsCount\n" +
            "FROM \n" +
            "    History h\n" +
            "LEFT JOIN Friendship f\n" +
            "ON (f.sender=h.starter AND f.receiver = h.invited) OR (f.sender=h.invited AND f.receiver = h.starter)\n" +
            "WHERE \n" +
            "    h.starter = ?1 OR h.invited=?1\n" +
            "ORDER BY h.creationDate DESC")
    Stream<HistoryProjection> findHistoryJoiningFriends(User user);

    @Query(value = "SELECT \n" +
            "   CASE WHEN h.starter=?1 THEN h.invited ELSE h.starter END AS oppUser,\n" +
            "   true as isFriend,\n" +
            "	h.creationDate AS creationDate,\n" +
            "	h.duration AS duration,\n" +
            "	h.wordsCount AS wordsCount\n" +
            "FROM \n" +
            "    History h\n" +
            "WHERE \n" +
            "    (h.starter = ?1 AND h.invited=?2) OR (h.starter =?2 AND h.invited=?1) \n" +
            "ORDER BY h.creationDate DESC")
    Stream<HistoryProjection> findHistoryWithOpp(User user, User opp);

}

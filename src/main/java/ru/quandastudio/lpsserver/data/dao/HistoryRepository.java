package ru.quandastudio.lpsserver.data.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.quandastudio.lpsserver.data.entities.HistoryItem;
import ru.quandastudio.lpsserver.data.entities.User;

@Repository
public interface HistoryRepository extends JpaRepository<HistoryItem, Long> {

	@SuppressWarnings("unchecked")
	public HistoryItem save(HistoryItem item);

	public List<HistoryItem> findFirst100ByStarterOrInvited(User starter, User invited);

}

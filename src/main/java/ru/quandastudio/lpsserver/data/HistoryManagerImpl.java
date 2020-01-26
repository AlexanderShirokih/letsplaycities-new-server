package ru.quandastudio.lpsserver.data;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.quandastudio.lpsserver.data.dao.HistoryRepository;
import ru.quandastudio.lpsserver.data.entities.HistoryItem;
import ru.quandastudio.lpsserver.data.entities.User;

@Service
@Transactional
public class HistoryManagerImpl implements HistoryManager {

	@Autowired
	private HistoryRepository historyDAO;

	@Override
	public void addHistoryItem(HistoryItem item) {
		historyDAO.save(item);
	}

	@Override
	public List<HistoryItem> getHistoryList(User user) {
		return historyDAO.findFirst100ByStarterOrInvited(user, user);
	}

}

package ru.quandastudio.lpsserver.data;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.quandastudio.lpsserver.data.dao.HistoryRepository;
import ru.quandastudio.lpsserver.data.entities.History;
import ru.quandastudio.lpsserver.data.entities.HistoryProjection;
import ru.quandastudio.lpsserver.data.entities.User;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class HistoryManagerImpl implements HistoryManager {

	private final HistoryRepository historyDAO;

	@Override
	public void addHistoryItem(History item) {
		historyDAO.save(item);
	}

	@Override
	public List<HistoryProjection> getHistoryList(User user) {
		return historyDAO.findHistoryJoiningFriends(user).limit(100).collect(Collectors.toList());
	}

}

package ru.quandastudio.lpsserver.data;

import java.util.List;

import ru.quandastudio.lpsserver.data.entities.HistoryProjection;
import ru.quandastudio.lpsserver.data.entities.History;
import ru.quandastudio.lpsserver.data.entities.User;

public interface HistoryManager {

	void addHistoryItem(History item);
	
	List<HistoryProjection> getHistoryList(User user);
}

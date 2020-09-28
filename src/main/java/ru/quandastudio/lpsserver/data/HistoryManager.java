package ru.quandastudio.lpsserver.data;

import java.util.List;

import ru.quandastudio.lpsserver.data.entities.HistoryProjection;
import ru.quandastudio.lpsserver.data.entities.History;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.HistoryInfo;

public interface HistoryManager {

	void addHistoryItem(History item);
	
	List<HistoryInfo> getHistoryList(User user);
}

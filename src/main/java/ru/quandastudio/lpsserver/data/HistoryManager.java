package ru.quandastudio.lpsserver.data;

import java.util.List;

import ru.quandastudio.lpsserver.data.entities.HistoryItem;
import ru.quandastudio.lpsserver.data.entities.User;

public interface HistoryManager {

	public void addHistoryItem(HistoryItem item);
	
	public List<HistoryItem> getHistoryList(User user);
}

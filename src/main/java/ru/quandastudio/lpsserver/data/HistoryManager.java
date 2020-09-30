package ru.quandastudio.lpsserver.data;

import ru.quandastudio.lpsserver.data.entities.History;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.HistoryInfo;

import java.util.List;

public interface HistoryManager {

    void addHistoryItem(History item);

    List<HistoryInfo> getHistoryList(User user);

    List<HistoryInfo> getHistoryListWithFriend(User user, User opp);
}

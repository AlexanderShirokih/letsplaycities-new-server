package ru.quandastudio.lpsserver.core;

import ru.quandastudio.lpsserver.data.entities.User;

public interface RequestNotifier {

	void sendNotification(User sender, User receiver);

}

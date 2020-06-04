package ru.quandastudio.lpsserver.core;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.quandastudio.lpsserver.data.entities.User;

public interface RequestNotifier {

	@AllArgsConstructor
	@Getter
	class NotificationData {
		final String title;
		final HashMap<String, String> params;
	}

	void sendNotification(User user, NotificationData data);

}

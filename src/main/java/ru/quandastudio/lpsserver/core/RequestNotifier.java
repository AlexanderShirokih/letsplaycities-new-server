package ru.quandastudio.lpsserver.core;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.quandastudio.lpsserver.data.entities.User;

public interface RequestNotifier {

	@AllArgsConstructor
	@Getter
	public class NotificationData {
		final String title;
		final HashMap<String, String> params;
	}

	public void sendNotification(User receiver, NotificationData data);

}

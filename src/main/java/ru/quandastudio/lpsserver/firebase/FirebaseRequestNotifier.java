package ru.quandastudio.lpsserver.firebase;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidConfig.Priority;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.RequestNotifier;
import ru.quandastudio.lpsserver.data.entities.User;

@Service
@RequiredArgsConstructor
public class FirebaseRequestNotifier implements RequestNotifier {

	private final FirebaseMessaging firebaseMessaging;

	@Override
	public void sendNotification(User sender, User receiver) {
		Message msg = buildMessage(sender.getUserId(), sender.getName(), receiver.getFirebaseToken());
		firebaseMessaging.sendAsync(msg);
	}

	private Message buildMessage(int userId, String login, String token) {
		return Message.builder()
				.putData("action", "fm_request")
				.putData("login", login)
				.putData("user_id", String.valueOf(userId))
				.setNotification(new Notification("Сыграем в Города",
						"Пользователь " + login + " приглашает вас сыграть в города."))
				.setAndroidConfig(AndroidConfig.builder()
						.setPriority(Priority.HIGH)
						.setTtl(0)
						.setRestrictedPackageName("ru.aleshi.letsplaycities")
						.setNotification(AndroidNotification.builder().setSound("default").build())
						.build())
				.setToken(token)
				.build();
	}

}

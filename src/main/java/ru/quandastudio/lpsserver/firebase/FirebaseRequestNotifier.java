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
	public void sendNotification(User receiver, NotificationData data) {
		Message msg = Message.builder()
				.putAllData(data.getParams())
				.setNotification(new Notification("Сыграем в Города", data.getTitle()))
				.setAndroidConfig(AndroidConfig.builder()
						.setPriority(Priority.HIGH)
						.setTtl(0)
						.setRestrictedPackageName("ru.aleshi.letsplaycities")
						.setNotification(AndroidNotification.builder().setSound("default").build())
						.build())
				.setToken(receiver.getFirebaseToken())
				.build();
		firebaseMessaging.sendAsync(msg);
	}

}

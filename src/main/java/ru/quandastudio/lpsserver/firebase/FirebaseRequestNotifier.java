package ru.quandastudio.lpsserver.firebase;

import com.google.firebase.messaging.*;
import com.google.firebase.messaging.AndroidConfig.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.quandastudio.lpsserver.core.notifications.RequestNotifier;
import ru.quandastudio.lpsserver.data.entities.User;

@Service
@RequiredArgsConstructor
public class FirebaseRequestNotifier implements RequestNotifier {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void sendNotification(User receiver, NotificationData data) {
        if (receiver.getFirebaseToken() == null || receiver.getFirebaseToken().isEmpty()) {
            return;
        }

        Message msg = Message.builder()
                .putAllData(data.getParams())
                .setNotification(
                        Notification.builder()
                                .setTitle("Сыграем в Города")
                                .setBody(data.getTitle())
                                .build()
                )
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

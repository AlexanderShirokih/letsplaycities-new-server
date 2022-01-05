package ru.quandastudio.lpsserver.firebase

import com.google.firebase.messaging.*
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import ru.quandastudio.lpsserver.core.notifications.DirectMessageNotifier
import ru.quandastudio.lpsserver.data.entities.User

@Service
@RequiredArgsConstructor
class FirebaseDirectMessageNotifier(
    private val firebaseMessaging: FirebaseMessaging,
) : DirectMessageNotifier {

    override fun sendNewDirectMessageNotification(
        receiver: User,
        notification: DirectMessageNotifier.NewDirectMessageNotification,
    ) {
        if (receiver.firebaseToken.isNullOrEmpty()) {
            return
        }

        val title = "Сообщение от пользователя ${notification.senderName}"

        val msg = Message.builder()
            .putAllData(
                mapOf(
                    "senderId" to notification.senderId.toString(),
                )
            )
            .setNotification(Notification(title, notification.shortContent))
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setTtl(0)
                    .setRestrictedPackageName("ru.aleshi.letsplaycities")
                    .setNotification(AndroidNotification.builder().setSound("default").build())
                    .build()
            )
            .setToken(receiver.firebaseToken)
            .build();

        firebaseMessaging.sendAsync(msg);
    }
}
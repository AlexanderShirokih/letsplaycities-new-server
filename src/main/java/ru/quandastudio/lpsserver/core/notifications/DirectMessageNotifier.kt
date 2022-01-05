package ru.quandastudio.lpsserver.core.notifications

import ru.quandastudio.lpsserver.data.entities.User

interface DirectMessageNotifier {

    class NewDirectMessageNotification(
        val senderId: Int,
        val senderName: String,
        val shortContent: String,
    )

    fun sendNewDirectMessageNotification(receiver: User, notification: NewDirectMessageNotification)
}
package ru.quandastudio.lpsserver.models.notifications

data class AddNotificationRequest(
    val targetId: Int? = null,
    val title: String? = null,
    val content: String,
)

package ru.quandastudio.lpsserver.models.notifications

data class NotificationItem(
    val id: Int,
    val isRead: Boolean,
    val title: String?,
    val content: String,
    val createdAt: Long,
)

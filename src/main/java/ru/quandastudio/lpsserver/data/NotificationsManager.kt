package ru.quandastudio.lpsserver.data

import ru.quandastudio.lpsserver.Result
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.models.notifications.AddNotificationRequest
import ru.quandastudio.lpsserver.models.notifications.NotificationItem

interface NotificationsManager {

    /**
     * Creates new notification.
     * @param actor Notification author
     * @param request Notification data
     */
    fun addNotification(actor: User, request: AddNotificationRequest): Result<String>

    /**
     * Deletes notification by ID.
     * @param actor user that has privileges to delete notification
     * @param notificationId Notification ID to be deleted
     */
    fun deleteNotification(actor: User, notificationId: Int): Result<String>

    /**
     * Gets notifications targeted for [user]
     * @param user notification receiver
     */
    fun getNotifications(user: User): List<NotificationItem>

    /**
     * Gets count on unread notifications for [user]
     */
    fun getUnreadNotificationCount(user: User): Long
}

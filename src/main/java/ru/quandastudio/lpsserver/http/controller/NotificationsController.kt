package ru.quandastudio.lpsserver.http.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ru.quandastudio.lpsserver.data.NotificationsManager
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.http.model.MessageWrapper
import ru.quandastudio.lpsserver.models.notifications.AddNotificationRequest
import ru.quandastudio.lpsserver.models.notifications.NotificationItem

@RestController
@RequestMapping("/notifications")
class NotificationsController(
    private val notificationsManager: NotificationsManager,
) {
    /**
     * Creates new city editing request.  Requires admin privileges.
     */
    @PostMapping
    fun createNotification(
        @RequestBody request: AddNotificationRequest,
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MessageWrapper<String>> {
        return notificationsManager
            .addNotification(
                actor = user,
                request = request,
            )
            .wrap()
            .toResponse()
    }

    /**
     * Deletes notification by its ID. Requires admin privileges.
     */
    @DeleteMapping("/{id}")
    fun deleteNotification(
        @PathVariable("id") notificationId: Int,
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MessageWrapper<String>> {
        return notificationsManager
            .deleteNotification(
                actor = user,
                notificationId = notificationId,
            )
            .wrap()
            .toResponse()
    }

    /**
     * Gets a list of notifications targeted for user
     */
    @GetMapping
    fun getNotificationsList(
        @AuthenticationPrincipal user: User,
    ): List<NotificationItem> = notificationsManager.getNotifications(user = user)

    /**
     * Gets a count of unread notifications
     */
    @GetMapping("/unread")
    fun getUnreadNotificationsCount(
        @AuthenticationPrincipal user: User,
    ): Long = notificationsManager.getUnreadNotificationCount(user = user)
}
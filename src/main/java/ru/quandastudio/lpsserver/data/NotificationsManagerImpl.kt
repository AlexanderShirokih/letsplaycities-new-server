package ru.quandastudio.lpsserver.data

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import ru.quandastudio.lpsserver.Result
import ru.quandastudio.lpsserver.data.dao.LastReadNotificationsRepository
import ru.quandastudio.lpsserver.data.dao.NotificationsRepository
import ru.quandastudio.lpsserver.data.entities.LastReadNotificationEntity
import ru.quandastudio.lpsserver.data.entities.NotificationEntity
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.models.Role
import ru.quandastudio.lpsserver.models.notifications.AddNotificationRequest
import ru.quandastudio.lpsserver.models.notifications.NotificationItem
import java.sql.Timestamp
import java.time.Instant
import javax.transaction.Transactional

@Service
@Transactional
open class NotificationsManagerImpl(
    private val notificationsRepository: NotificationsRepository,
    private val lastReadNotificationsRepository: LastReadNotificationsRepository,
) : NotificationsManager {

    override fun addNotification(actor: User, request: AddNotificationRequest): Result<String> {
        if (actor.role != Role.ADMIN) {
            return Result.error("Permission denied!")
        }

        notificationsRepository
            .save(
                NotificationEntity(
                    id = 0,
                    creationDate = Timestamp.from(Instant.now()),
                    target = request.targetId?.let { User(it) },
                    content = request.content,
                    title = request.title,
                )
            )

        return Result.success("ok")
    }

    override fun deleteNotification(actor: User, notificationId: Int): Result<String> {
        if (actor.role != Role.ADMIN) {
            return Result.error("Permission denied!")
        }

        if (notificationsRepository.existsById(notificationId)) {
            notificationsRepository.deleteById(notificationId)
        } else {
            return Result.error("Entity with id=$notificationId doesnt exists")
        }

        return Result.success("ok")
    }

    override fun getNotifications(user: User): List<NotificationItem> {
        val lastRead = lastReadNotificationsRepository.findByIdOrNull(user.id)
        val lastReadId = lastRead?.lastReadNotificationId ?: 0

        val notifications = notificationsRepository
            .findTop30ByTargetIsNullOrTargetIsOrderByCreationDateDesc(user)

        val topNotification = notifications.maxByOrNull { it.id }

        // Update last read notification
        if (topNotification != null && topNotification.id != lastReadId) {
            if (lastRead == null) {
                lastReadNotificationsRepository.save(
                    LastReadNotificationEntity(userId = user.id, lastReadNotificationId = topNotification.id)
                )
            } else {
                lastReadNotificationsRepository.save(
                    lastRead.apply {
                        lastReadNotificationId = topNotification.id
                    }
                )
            }
        }

        return notifications.map {
            NotificationItem(
                id = it.id,
                isRead = it.id <= lastReadId,
                title = it.title,
                content = it.content,
                createdAt = it.creationDate.time,
            )
        }
    }

    override fun getUnreadNotificationCount(user: User): Long {
        val lastRead = lastReadNotificationsRepository.findByIdOrNull(user.id)
        val lastReadId = lastRead?.lastReadNotificationId ?: 0

        return notificationsRepository.countByIdAfter(lastReadId)
    }
}
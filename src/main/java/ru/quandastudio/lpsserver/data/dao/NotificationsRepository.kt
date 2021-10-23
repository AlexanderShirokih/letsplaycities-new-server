package ru.quandastudio.lpsserver.data.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.quandastudio.lpsserver.data.entities.NotificationEntity
import ru.quandastudio.lpsserver.data.entities.User

@Repository
interface NotificationsRepository : JpaRepository<NotificationEntity, Int> {

    fun findTop30ByTargetIsNullOrTargetIsOrderByCreationDateDesc(target: User): List<NotificationEntity>

    fun countByIdAfter(targetId: Int): Long
}
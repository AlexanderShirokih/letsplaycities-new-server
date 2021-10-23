package ru.quandastudio.lpsserver.data.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.quandastudio.lpsserver.data.entities.LastReadNotificationEntity

@Repository
interface LastReadNotificationsRepository : JpaRepository<LastReadNotificationEntity, Int>

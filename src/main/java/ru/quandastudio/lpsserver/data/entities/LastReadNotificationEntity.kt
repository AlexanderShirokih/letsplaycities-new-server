package ru.quandastudio.lpsserver.data.entities

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "LastReadNotification")
class LastReadNotificationEntity(
    @Id
    @Column(name = "user_id")
    var userId: Int,

    @Column(name = "last_read_id", nullable = false)
    var lastReadNotificationId: Int,
) : Serializable

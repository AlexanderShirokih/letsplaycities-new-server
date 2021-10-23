package ru.quandastudio.lpsserver.data.entities

import org.hibernate.annotations.CreationTimestamp
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "Notification")
class NotificationEntity(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = true)
    var target: User? = null,

    @Column(name = "title", nullable = true)
    var title: String? = null,

    @Column(name = "content", nullable = false)
    var content: String,

    @CreationTimestamp
    @Column(name = "creation_date")
    var creationDate: Timestamp
) : Serializable
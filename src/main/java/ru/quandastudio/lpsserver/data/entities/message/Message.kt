package ru.quandastudio.lpsserver.data.entities.message

import org.hibernate.annotations.CreationTimestamp
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.data.entities.chat.Chat
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "Message")
class Message(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    var chat: Chat? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    var author: User? = null,

    @Column(name = "content", nullable = true, length = 240)
    var content: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    var createdAt: Timestamp? = null,
)

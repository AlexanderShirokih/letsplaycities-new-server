package ru.quandastudio.lpsserver.data.entities.chat

import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.data.entities.message.Message
import javax.persistence.*

@Entity
@IdClass(ChatParticipantKey::class)
class ChatParticipant(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    var chat: Chat? = null,

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    var participant: User? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false)
    var type: PermissionLevel? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id", nullable = true)
    var lastReadMessage: Message? = null,
)

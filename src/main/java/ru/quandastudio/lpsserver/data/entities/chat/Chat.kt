package ru.quandastudio.lpsserver.data.entities.chat

import javax.persistence.*

@Entity
@Table(name = "Chat")
class Chat(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: ChatType? = null,

    @Column(name = "name", nullable = true, length = 64)
    var name: String? = null,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "chat")
    val participants: Set<ChatParticipant>,
)

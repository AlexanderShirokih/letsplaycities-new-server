package ru.quandastudio.lpsserver.data.entities.chat

import ru.quandastudio.lpsserver.data.entities.User
import java.io.Serializable

class ChatParticipantKey(
    var chat: Chat? = null,
    var participant: User? = null,
) : Serializable

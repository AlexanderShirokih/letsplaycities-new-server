package ru.quandastudio.lpsserver.models.chat

import ru.quandastudio.lpsserver.models.messages.MessageInfo

/**
 * Chat entry
 */
data class ChatEntry(
    /**
     * Recipient user ID
     */
    val targetId: Int,

    /**
     * Is all messages in the chat are read
     */
    val isRead: Boolean,

    /**
     * Latest message sent to this chat
     */
    val latestMessage: MessageInfo,
)

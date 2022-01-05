package ru.quandastudio.lpsserver.models.messages

/**
 * Describes user message
 */
data class MessageInfo(
    /**
     * Message ID,
     */
    val messageId: Long,
    /**
     * User who sends the message
     */
    val authorId: Int,

    /**
     * `true` if message was read by recipient, `false` if not
     */
    val isRead: Boolean,

    /**
     * Message posting timestamp millis
     */
    val postedAt: Long,
    /**
     * Message content
     */
    val content: String,
)

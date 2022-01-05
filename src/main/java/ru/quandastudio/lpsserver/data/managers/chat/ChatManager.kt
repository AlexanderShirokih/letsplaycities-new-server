package ru.quandastudio.lpsserver.data.managers.chat

import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.http.model.MessageWrapper
import ru.quandastudio.lpsserver.models.chat.ChatEntry

interface ChatManager {

    /**
     * Gets a count of unread messages
     */
    fun getUnreadMessagesCount(user: User): Int

    /**
     * Gets all chats in which user participates
     */
    fun getChats(user: User): MessageWrapper<List<ChatEntry>>
}
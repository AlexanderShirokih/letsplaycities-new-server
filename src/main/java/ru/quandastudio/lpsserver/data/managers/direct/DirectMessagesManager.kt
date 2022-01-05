package ru.quandastudio.lpsserver.data.managers.direct

import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.http.model.MessageWrapper
import ru.quandastudio.lpsserver.models.messages.GetMessageListRequest
import ru.quandastudio.lpsserver.models.messages.MessageInfo
import ru.quandastudio.lpsserver.models.messages.MessageList
import ru.quandastudio.lpsserver.models.messages.SendMessageRequest

interface DirectMessagesManager {
    /**
     * Posts message to the database and notifies the receiver about a new message.
     */
    fun sendMessage(sender: User, messageRequest: SendMessageRequest): MessageWrapper<MessageInfo>

    /**
     * Gets a list of direct messages sent or received by [sender]
     */
    fun getMessages(sender: User, request: GetMessageListRequest): MessageWrapper<MessageList>
}
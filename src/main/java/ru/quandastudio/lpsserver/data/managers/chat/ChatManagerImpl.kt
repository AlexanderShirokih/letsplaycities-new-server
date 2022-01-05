package ru.quandastudio.lpsserver.data.managers.chat

import org.springframework.stereotype.Service
import ru.quandastudio.lpsserver.Result
import ru.quandastudio.lpsserver.data.dao.message.MessageRepository
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.http.model.MessageWrapper
import ru.quandastudio.lpsserver.models.chat.ChatEntry

@Service
class ChatManagerImpl(
    private val directMessagesRepository: MessageRepository
) : ChatManager {

    override fun getUnreadMessagesCount(user: User): Int {
        // TODO: Implement
        return 0
//        return directMessagesRepository.countByRecipientAndIsReadIsFalse(user).toInt()
    }

    override fun getChats(user: User): MessageWrapper<List<ChatEntry>> {
        // TODO: Take chats from direct messages
        return Result.error<List<ChatEntry>>("Not allowed!").wrap()
    }
}
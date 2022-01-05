package ru.quandastudio.lpsserver.data.managers.direct

import org.springframework.stereotype.Service
import ru.quandastudio.lpsserver.Result
import ru.quandastudio.lpsserver.core.notifications.DirectMessageNotifier
import ru.quandastudio.lpsserver.data.BlacklistManager
import ru.quandastudio.lpsserver.data.UserManager
import ru.quandastudio.lpsserver.data.dao.message.MessageRepository
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.data.entities.message.Message
import ru.quandastudio.lpsserver.http.model.MessageWrapper
import ru.quandastudio.lpsserver.models.messages.GetMessageListRequest
import ru.quandastudio.lpsserver.models.messages.MessageInfo
import ru.quandastudio.lpsserver.models.messages.MessageList
import ru.quandastudio.lpsserver.models.messages.SendMessageRequest
import ru.quandastudio.lpsserver.util.ValidationUtil
import java.time.Instant

@Service
class DirectMessagesManagerImpl(
    private val directMessagesRepository: MessageRepository,
    private val directMessageNotifier: DirectMessageNotifier,
    private val blacklistManager: BlacklistManager,
    private val userManager: UserManager,
) : DirectMessagesManager {

    override fun sendMessage(sender: User, messageRequest: SendMessageRequest): MessageWrapper<MessageInfo> {
        val firstError = ValidationUtil.validateMessage(messageRequest)
        if (firstError.isPresent) {
            return Result.error<MessageInfo>(firstError.get()).wrap()
        }

        val recipient = userManager.getUserById(messageRequest.targetId).orElse(null)
            ?: return Result.error<MessageInfo>("Пользователь не найден!").wrap()

        if (blacklistManager.isBanned(sender, recipient)) {
            return Result.error<MessageInfo>("Нельзя отправлять сообщение заблокированному пользователю").wrap()
        }

        // TODO: Get chat
        if (true) {
            return Result.error<MessageInfo>("Not allowed!").wrap()
        }

        val message = directMessagesRepository.save(
            Message(
                author = sender,
//                recipient = recipient,
                content = messageRequest.content,
            )
        )

        directMessageNotifier.sendNewDirectMessageNotification(
            receiver = recipient,
            notification = DirectMessageNotifier.NewDirectMessageNotification(
                senderId = sender.id,
                senderName = sender.name,
                shortContent = message.content.orEmpty().take(40),
            )
        )

        return Result.success(
            MessageInfo(
                messageId = message.id ?: 0L,
                authorId = sender.id,
                isRead = true,
                postedAt = (message.createdAt?.toInstant() ?: Instant.now()).toEpochMilli(),
                content = message.content.orEmpty(),
            )
        ).wrap()
    }

    override fun getMessages(sender: User, request: GetMessageListRequest): MessageWrapper<MessageList> {
        val firstError = ValidationUtil.validateMessage(request)
        if (firstError.isPresent) {
            return Result.error<MessageList>(firstError.get()).wrap()
        }

        val pageSize = 40

        return Result.error<MessageList>("Not allowed").wrap()
//        val dbMessages = directMessagesRepository.findTopNBySenderOrRecipient(sender, request.offset, pageSize)
//
//        val messages = dbMessages.map {
//            MessageInfo(
//                messageId = it.id ?: 0L,
//                isRead = it.sender?.id == sender.id || it.isRead ?: false,
//                authorId = it.sender?.id ?: 0,
//                postedAt = it.creationDate?.time ?: 0L,
//                content = it.content.orEmpty(),
//            )
//        }
//
//        val unreadMessages = dbMessages
//            .filterUnreadMessageFor(sender)
//            .onEach { it.isRead = true }
//            .toList()
//
//        directMessagesRepository.saveAll(unreadMessages)
//
//        return Result.success(
//            MessageList(
//                pageSize = pageSize,
//                messages = messages,
//            )
//        ).wrap()
    }

//    private fun List<Message>.filterUnreadMessageFor(sender: User): Sequence<DirectMessage> {
//        val senderId = sender.id
//        return this
//            .asSequence()
//            .filter { it.isRead != true }
//            .filter { it.recipient?.id == senderId }
//    }

}

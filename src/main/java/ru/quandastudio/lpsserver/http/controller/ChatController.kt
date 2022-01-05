package ru.quandastudio.lpsserver.http.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.data.managers.chat.ChatManager
import ru.quandastudio.lpsserver.http.model.MessageWrapper
import ru.quandastudio.lpsserver.models.chat.ChatEntry

@RestController
@RequestMapping("/chat")
class ChatController(
    private val chatManager: ChatManager,
) {

    /**
     * Gets a list of chats
     */
    @GetMapping
    fun getChats(
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MessageWrapper<List<ChatEntry>>> {
        return chatManager
            .getChats(user)
            .toResponse()
    }

    /**
     * Gets a count of unread messages summarized by all chats
     */
    @GetMapping("/unread")
    @ResponseBody
    fun getUnreadMessagesCount(
        @AuthenticationPrincipal user: User,
    ): Int {
        return chatManager.getUnreadMessagesCount(user)
    }
}
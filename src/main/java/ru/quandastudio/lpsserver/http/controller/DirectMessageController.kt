package ru.quandastudio.lpsserver.http.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.data.managers.direct.DirectMessagesManager
import ru.quandastudio.lpsserver.http.model.MessageWrapper
import ru.quandastudio.lpsserver.models.messages.GetMessageListRequest
import ru.quandastudio.lpsserver.models.messages.MessageInfo
import ru.quandastudio.lpsserver.models.messages.MessageList
import ru.quandastudio.lpsserver.models.messages.SendMessageRequest

@RestController
@RequestMapping("/dm")
class DirectMessageController(
    private val directMessagesManager: DirectMessagesManager,
) {

    /**
     * Sends a new message to user.
     * @return posted message
     */
    @PostMapping
    fun sendMessage(
        @RequestBody request: SendMessageRequest,
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MessageWrapper<MessageInfo>> {
        return directMessagesManager.sendMessage(
            sender = user,
            messageRequest = request
        ).toResponse()
    }

    /**
     * Gets all messages sent to the user [GetMessageListRequest.targetId] with offset
     */
    @GetMapping
    @ResponseBody
    fun getMessages(
        @RequestParam targetId: Int,
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MessageWrapper<MessageList>> {
        return directMessagesManager.getMessages(
            sender = user,
            request = GetMessageListRequest(
                targetId = targetId,
                offset = offset,
            ),
        ).toResponse()
    }


}
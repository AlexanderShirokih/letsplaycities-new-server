package ru.quandastudio.lpsserver.http.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.data.managers.room.request.RoomRequestManager
import ru.quandastudio.lpsserver.http.model.MessageWrapper
import ru.quandastudio.lpsserver.models.room.request.CreateRoomRequest
import ru.quandastudio.lpsserver.models.room.request.RoomRequestResult

@RestController
@RequestMapping("/room/")
class RoomController(
    private val roomRequestManager: RoomRequestManager,
) {

    /**
     * Creates new room request
     */
    @PostMapping("/create")
    fun createRoomRequest(
        @RequestBody request: CreateRoomRequest,
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<MessageWrapper<RoomRequestResult>> {
        return roomRequestManager.createRoomRequest(request, user)
            .wrap()
            .toResponse()
    }
}
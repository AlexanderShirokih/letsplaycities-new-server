package ru.quandastudio.lpsserver.data.managers.room.request

import ru.quandastudio.lpsserver.Result
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.models.room.request.CreateRoomRequest
import ru.quandastudio.lpsserver.models.room.request.RoomRequestResult

interface RoomRequestManager {

    /**
     * Creates new request
     */
    fun createRoomRequest(request: CreateRoomRequest, user: User): Result<RoomRequestResult>
}
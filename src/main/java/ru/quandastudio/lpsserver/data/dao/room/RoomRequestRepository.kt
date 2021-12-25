package ru.quandastudio.lpsserver.data.dao.room

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.data.entities.room.RoomRequestEntity
import ru.quandastudio.lpsserver.data.entities.room.RoomRequestStatusEntity

@Repository
interface RoomRequestRepository : JpaRepository<RoomRequestEntity, Int> {

    fun countAllByRequesterAndStatus(requester: User, status: RoomRequestStatusEntity): Int
}
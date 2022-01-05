package ru.quandastudio.lpsserver.data.managers.room.request

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import ru.quandastudio.lpsserver.Result
import ru.quandastudio.lpsserver.core.game.id.GameIdFactory
import ru.quandastudio.lpsserver.data.dao.country.CountryGroupRepository
import ru.quandastudio.lpsserver.data.dao.room.RoomRequestRepository
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.data.entities.room.RoomRequestEntity
import ru.quandastudio.lpsserver.data.entities.room.RoomRequestStatusEntity
import ru.quandastudio.lpsserver.data.mappers.country.CountryGroupMapper
import ru.quandastudio.lpsserver.models.room.request.CreateRoomRequest
import ru.quandastudio.lpsserver.models.room.request.RoomRequestResult

@Service
class RoomRequestManagerImpl(
    private val roomRequestRepository: RoomRequestRepository,
    private val countryGroupRepository: CountryGroupRepository,
    private val countryGroupMapper: CountryGroupMapper,
    private val gameIdFactory: GameIdFactory,
) : RoomRequestManager {

    override fun createRoomRequest(
        request: CreateRoomRequest,
        user: User,
    ): Result<RoomRequestResult> {
        val currentlyOpenedRequests = roomRequestRepository.countAllByRequesterAndStatus(
            status = RoomRequestStatusEntity.NEW,
            requester = user,
        )

        if (currentlyOpenedRequests > OPENED_REQUESTS_LIMIT) {
            return Result.error("В данный момент у вас слишком много открытых заявок.")
        }

        val group = request.countryGroupId?.let { countryGroupRepository.findByIdOrNull(it) }

        val newRequest = roomRequestRepository.save(
            RoomRequestEntity(
                requester = user,
                countryGroup = group,
            )
        )

        val battleId = requireNotNull(newRequest.id)
        val hash = gameIdFactory.createBattleHash(battleId.toLong())

        // Update cache
        newRequest.hash = hash
        roomRequestRepository.save(newRequest)

        val url = "/game/$hash"

        return Result.success(
            RoomRequestResult(
                id = battleId,
                relativeLinkUrl = url,
                expiryTime = EXPIRY_TIME_MS,
                creationTimestamp = requireNotNull(newRequest.creationDate).time,
                countryGroup = newRequest.countryGroup?.let { countryGroupMapper.toModel(it) },
            )
        )
    }

    companion object {
        private const val OPENED_REQUESTS_LIMIT = 30

        private const val EXPIRY_TIME_MS = 30 * 60 * 1000L
    }
}

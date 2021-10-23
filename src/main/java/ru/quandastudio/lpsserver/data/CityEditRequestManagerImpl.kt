package ru.quandastudio.lpsserver.data

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import ru.quandastudio.lpsserver.Result
import ru.quandastudio.lpsserver.data.dao.CityEditRequestRepository
import ru.quandastudio.lpsserver.data.entities.CityEditRequestEntity
import ru.quandastudio.lpsserver.data.entities.CityEditRequestStatusEntity
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.models.Role
import ru.quandastudio.lpsserver.models.cities.CityEditRequest
import ru.quandastudio.lpsserver.models.cities.CityEditRequestStatus
import ru.quandastudio.lpsserver.models.cities.CityEditResult
import javax.transaction.Transactional

@Service
@Transactional
open class CityEditRequestManagerImpl(
    private val cityEditRequestRepository: CityEditRequestRepository,
) : CityEditRequestManager {

    override fun addCityEditRequest(owner: User, request: CityEditRequest): Result<String> {
        val currentLimit = cityEditRequestRepository.countAllByOwnerAndStatus(owner, CityEditRequestStatusEntity.NEW)

        if (currentLimit > ACTIVE_REQUESTS_LIMIT) {
            return Result.error("Превышен лимит открытых заявок. Подождите пока текущие заявки будут обработаны")
        }

        cityEditRequestRepository.save(
            CityEditRequestEntity(
                owner = owner,
                countryCode = request.countryCode,
                oldName = request.oldName,
                newName = request.newName,
                reason = request.reason,
            )
        )

        return Result.success("ok")
    }

    override fun deleteRequest(requestId: Int, owner: User) {
        cityEditRequestRepository.deleteByIdAndOwner(requestId, owner)
    }

    override fun getRequestList(owner: User): List<CityEditResult> {
        return cityEditRequestRepository
            .findTop50ByOwnerOrderByStatusAsc(owner)
            .map { it.toModel() }
    }

    override fun getOpenedRequests(actor: User): Result<List<CityEditResult>> {
        return if (actor.role == Role.ADMIN) {
            cityEditRequestRepository
                .findAllByStatus(status = CityEditRequestStatusEntity.NEW)
                .map { it.toModel() }
                .let { Result.success(it) }
        } else {
            Result.error("Permission denied!")
        }
    }

    override fun updateStatus(requestId: Int, actor: User, status: CityEditRequestStatus): Result<String> {
        return if (actor.role == Role.ADMIN) {
            val request =
                cityEditRequestRepository.findByIdOrNull(requestId) ?: return Result.error("Request is not found!")
            cityEditRequestRepository.save(request.apply {
                this.status = status.toEntity()
            })
            Result.success("ok")
        } else {
            Result.error("Invalid grant!")
        }
    }

    private fun CityEditRequestStatus.toEntity() = when (this) {
        CityEditRequestStatus.NEW -> CityEditRequestStatusEntity.NEW
        CityEditRequestStatus.APPROVED -> CityEditRequestStatusEntity.APPROVED
        CityEditRequestStatus.DECLINED -> CityEditRequestStatusEntity.DECLINED
    }

    private fun CityEditRequestStatusEntity.toModel() = when (this) {
        CityEditRequestStatusEntity.NEW -> CityEditRequestStatus.NEW
        CityEditRequestStatusEntity.APPROVED -> CityEditRequestStatus.APPROVED
        CityEditRequestStatusEntity.DECLINED -> CityEditRequestStatus.DECLINED
    }

    private fun CityEditRequestEntity.toModel() = CityEditResult(
        id = requireNotNull(id),
        countryCode = countryCode,
        oldName = oldName,
        newName = newName,
        reason = reason,
        verdict = verdict,
        status = status.toModel(),
    )

    companion object {
        private const val ACTIVE_REQUESTS_LIMIT = 50
    }
}
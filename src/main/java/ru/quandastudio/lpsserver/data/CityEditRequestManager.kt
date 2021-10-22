package ru.quandastudio.lpsserver.data

import ru.quandastudio.lpsserver.Result
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.models.cities.CityEditRequest
import ru.quandastudio.lpsserver.models.cities.CityEditRequestStatus
import ru.quandastudio.lpsserver.models.cities.CityEditResult

interface CityEditRequestManager {

    /**
     * Inserts new city request
     */
    fun addCityEditRequest(owner: User, request: CityEditRequest): Result<String>

    /**
     * Deletes request
     */
    fun deleteRequest(requestId: Int, owner: User)

    /**
     * Gets request list for [owner]
     */
    fun getRequestList(owner: User): List<CityEditResult>

    /**
     * Updates city edit request status
     */
    fun updateStatus(requestId: Int, actor: User, status: CityEditRequestStatus): Result<String>
}

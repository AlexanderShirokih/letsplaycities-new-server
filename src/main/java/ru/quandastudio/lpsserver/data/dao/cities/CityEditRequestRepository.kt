package ru.quandastudio.lpsserver.data.dao.cities

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.quandastudio.lpsserver.data.entities.cities.CityEditRequestEntity
import ru.quandastudio.lpsserver.data.entities.cities.CityEditRequestStatusEntity
import ru.quandastudio.lpsserver.data.entities.User

@Repository
interface CityEditRequestRepository : JpaRepository<CityEditRequestEntity, Int> {
    /**
     * Counts all requests by owner and status
     */
    fun countAllByOwnerAndStatus(owner: User, status: CityEditRequestStatusEntity): Long

    /**
     * Finds top 50 request by [owner] ordered by status
     */
    fun findTop50ByOwnerOrderByStatusAsc(owner: User): List<CityEditRequestEntity>

    /**
     * Gets all requests by certain status
     */
    fun findAllByStatus(status: CityEditRequestStatusEntity): List<CityEditRequestEntity>

    /**
     * Deletes request by its ID and owner
     */
    fun deleteByIdAndOwner(id: Int, owner: User)

}
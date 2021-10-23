package ru.quandastudio.lpsserver.data.dao;

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.quandastudio.lpsserver.data.entities.CityEditRequestEntity
import ru.quandastudio.lpsserver.data.entities.CityEditRequestStatusEntity
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
     * Deletes request by its ID and owner
     */
    fun deleteByIdAndOwner(id: Int, owner: User)
}
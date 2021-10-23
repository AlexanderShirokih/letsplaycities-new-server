package ru.quandastudio.lpsserver.data.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.quandastudio.lpsserver.data.entities.ForbiddenLoginEntity

@Repository
interface ForbiddenLoginRepository : JpaRepository<ForbiddenLoginEntity, Int> {
    fun existsByValue(value: String): Boolean
}
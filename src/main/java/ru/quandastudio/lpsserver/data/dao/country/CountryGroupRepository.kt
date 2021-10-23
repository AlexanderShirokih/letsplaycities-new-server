package ru.quandastudio.lpsserver.data.dao.country

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.quandastudio.lpsserver.data.entities.cities.CountryGroupEntity

@Repository
interface CountryGroupRepository : JpaRepository<CountryGroupEntity, Int>
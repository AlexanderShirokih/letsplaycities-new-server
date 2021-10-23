package ru.quandastudio.lpsserver.data.dao.country

import org.springframework.data.jpa.repository.JpaRepository
import ru.quandastudio.lpsserver.data.entities.cities.CountryEntity

interface CountryRepository : JpaRepository<CountryEntity, Int>
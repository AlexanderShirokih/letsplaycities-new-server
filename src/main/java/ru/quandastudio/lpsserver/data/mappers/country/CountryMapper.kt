package ru.quandastudio.lpsserver.data.mappers.country

import ru.quandastudio.lpsserver.data.entities.cities.CountryEntity
import ru.quandastudio.lpsserver.models.country.Country

class CountryMapper {
    fun toModel(entity: CountryEntity): Country = Country(
        id = requireNotNull(entity.id),
        name = requireNotNull(entity.name),
    )
}
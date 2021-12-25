package ru.quandastudio.lpsserver.data.mappers.country

import ru.quandastudio.lpsserver.data.entities.cities.CountryGroupEntity
import ru.quandastudio.lpsserver.models.country.CountryGroup

class CountryGroupMapper(
    private val countryMapper: CountryMapper,
) {

    fun toModel(entity: CountryGroupEntity): CountryGroup {
        return CountryGroup(
            id = requireNotNull(entity.id),
            name = requireNotNull(entity.name),
            countries = entity.countries.map { countryMapper.toModel(it) }
        )
    }
}
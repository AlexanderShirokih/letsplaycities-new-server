package ru.quandastudio.lpsserver.data.managers.country

import org.springframework.stereotype.Service
import ru.quandastudio.lpsserver.data.dao.country.CountryGroupRepository
import ru.quandastudio.lpsserver.data.dao.country.CountryRepository
import ru.quandastudio.lpsserver.data.entities.cities.CountryEntity
import ru.quandastudio.lpsserver.data.entities.cities.CountryGroupEntity
import ru.quandastudio.lpsserver.models.country.Country
import ru.quandastudio.lpsserver.models.country.CountryGroup

@Service
class CountryManagerImpl(
    private val countryRepository: CountryRepository,
    private val countryGroupRepository: CountryGroupRepository,
) : CountryManager {

    override fun getCountryList(): List<Country> {
        return countryRepository.findAll().map { it.toModel() }
    }

    override fun getCountryGroup(): List<CountryGroup> {
        return countryGroupRepository.findAll().map { it.toModel() }
    }

    private fun CountryEntity.toModel() = Country(
        id = requireNotNull(id),
        name = requireNotNull(name),
    )

    private fun CountryGroupEntity.toModel() = CountryGroup(
        id = requireNotNull(id),
        name = requireNotNull(name),
        countries = countries.map { it.toModel() }
    )
}
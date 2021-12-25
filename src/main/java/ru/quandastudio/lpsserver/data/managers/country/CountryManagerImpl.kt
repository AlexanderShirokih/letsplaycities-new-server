package ru.quandastudio.lpsserver.data.managers.country

import org.springframework.stereotype.Service
import ru.quandastudio.lpsserver.data.dao.country.CountryGroupRepository
import ru.quandastudio.lpsserver.data.dao.country.CountryRepository
import ru.quandastudio.lpsserver.data.mappers.country.CountryGroupMapper
import ru.quandastudio.lpsserver.data.mappers.country.CountryMapper
import ru.quandastudio.lpsserver.models.country.Country
import ru.quandastudio.lpsserver.models.country.CountryGroup

@Service
class CountryManagerImpl(
    private val countryRepository: CountryRepository,
    private val countryGroupRepository: CountryGroupRepository,
    private val countryGroupMapper: CountryGroupMapper,
    private val countryMapper: CountryMapper,
) : CountryManager {

    override fun getCountryList(): List<Country> {
        return countryRepository.findAll().map { countryMapper.toModel(it) }
    }

    override fun getCountryGroup(): List<CountryGroup> {
        return countryGroupRepository.findAll().map { countryGroupMapper.toModel(it) }
    }
}

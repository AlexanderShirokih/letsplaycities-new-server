package ru.quandastudio.lpsserver.data.managers.country

import ru.quandastudio.lpsserver.models.country.Country
import ru.quandastudio.lpsserver.models.country.CountryGroup

interface CountryManager {
    /**
     * Gets a list of all countries
     */
    fun getCountryList(): List<Country>

    /**
     * Gets a list of all country groups
     */
    fun getCountryGroup(): List<CountryGroup>
}

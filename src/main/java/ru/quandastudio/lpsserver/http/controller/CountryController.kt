package ru.quandastudio.lpsserver.http.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.quandastudio.lpsserver.data.managers.country.CountryManager
import ru.quandastudio.lpsserver.models.country.Country
import ru.quandastudio.lpsserver.models.country.CountryGroup

@RestController
@RequestMapping("/countries")
class CountryController(
    private val countryManager: CountryManager,
) {

    /**
     * Gets a list of countries
     */
    @GetMapping
    fun getCountryList(): List<Country> = countryManager.getCountryList()

    /**
     * Gets a list of country group
     */
    @GetMapping("/groups/")
    fun getCountryGroups(): List<CountryGroup> = countryManager.getCountryGroup()
}

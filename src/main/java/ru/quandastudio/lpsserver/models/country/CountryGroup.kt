package ru.quandastudio.lpsserver.models.country

data class CountryGroup(
    val id: Int,
    val name: String,
    val countries: List<Country>,
)

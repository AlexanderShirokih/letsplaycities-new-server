package ru.quandastudio.lpsserver.models.cities

data class CityEditRequest(
    var countryCode: Int,
    var oldName: String? = null,
    var newName: String? = null,
    var reason: String? = null,
)

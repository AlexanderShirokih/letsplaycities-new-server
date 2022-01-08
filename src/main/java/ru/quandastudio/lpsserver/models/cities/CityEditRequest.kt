package ru.quandastudio.lpsserver.models.cities

data class CityEditRequest(
    var oldCountryCode: Int = 0,
    var newCountryCode: Int = 0,
    var oldName: String? = null,
    var newName: String? = null,
    var reason: String? = null,
)

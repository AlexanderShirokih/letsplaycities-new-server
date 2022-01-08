package ru.quandastudio.lpsserver.models.cities

data class CityEditResult(
    val id: Int,
    val oldCountryCode: Int,
    val newCountryCode: Int,
    val oldName: String?,
    val newName: String?,
    val reason: String?,
    val verdict: String?,
    val status: CityEditRequestStatus,
)

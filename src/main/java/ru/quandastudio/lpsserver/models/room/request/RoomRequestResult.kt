package ru.quandastudio.lpsserver.models.room.request

import ru.quandastudio.lpsserver.models.country.CountryGroup

data class RoomRequestResult(
    val id: Int,
    val relativeLinkUrl: String,
    val expiryTime: Long,
    val creationTimestamp: Long,
    val countryGroup: CountryGroup? = null,
)

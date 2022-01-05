package ru.quandastudio.lpsserver.models.messages

import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

data class GetMessageListRequest(
    /**
     * Message recipient ID
     */
    @field:NotNull
    @field:Positive
    val targetId: Int,

    /**
     * Message list offset from the latest message
     */
    @field:PositiveOrZero
    val offset: Int = 0,
)

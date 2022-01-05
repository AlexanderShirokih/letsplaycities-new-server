package ru.quandastudio.lpsserver.models.messages

import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive
import javax.validation.constraints.Size

data class SendMessageRequest(
    @field:NotNull
    @field:Positive
    val targetId: Int,
    @field:Size(min = 1, max = 240)
    val content: String,
)

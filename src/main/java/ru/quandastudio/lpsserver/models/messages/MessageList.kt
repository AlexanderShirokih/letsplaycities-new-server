package ru.quandastudio.lpsserver.models.messages

data class MessageList(
    val pageSize: Int,
    val messages: List<MessageInfo>,
)
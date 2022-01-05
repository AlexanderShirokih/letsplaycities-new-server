package ru.quandastudio.lpsserver.data.dao.chat

import org.springframework.data.jpa.repository.JpaRepository
import ru.quandastudio.lpsserver.data.entities.chat.Chat

interface ChatRepository : JpaRepository<Chat, Long> {
}
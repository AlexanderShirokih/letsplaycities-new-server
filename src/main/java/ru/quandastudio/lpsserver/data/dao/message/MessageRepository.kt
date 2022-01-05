package ru.quandastudio.lpsserver.data.dao.message

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.quandastudio.lpsserver.data.entities.message.Message

@Repository
interface MessageRepository : JpaRepository<Message, Long>
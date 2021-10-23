package ru.quandastudio.lpsserver.data

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.quandastudio.lpsserver.data.dao.ForbiddenLoginRepository

@Service
open class ForbiddenLoginManagerImpl(
    private val forbiddenLoginRepository: ForbiddenLoginRepository,
) : ForbiddenLoginManager {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun isLoginValid(value: String): Boolean {
        val isForbidden = value.isBlank() || value
            .splitToSequence(" ")
            .any { word -> forbiddenLoginRepository.existsByValue(word.lowercase()) }

        if (isForbidden) {
            log.info("Forbidden login detected! Login was: {}", value)
        }

        return !isForbidden
    }
}
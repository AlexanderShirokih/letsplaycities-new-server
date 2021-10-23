package ru.quandastudio.lpsserver.data

interface ForbiddenLoginManager {

    /**
     * Checks login validity
     * @return `true` if login is valid, `false` otherwise
     */
    fun isLoginValid(value: String): Boolean
}
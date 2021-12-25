package ru.quandastudio.lpsserver.core.game.id

import org.apache.commons.codec.digest.DigestUtils


class SimpleGameIdFactory : GameIdFactory {
    override fun createBattleHash(battleId: Long): String {
        return DigestUtils.sha1Hex(battleId.toString())
            .reversed()
            .map { REPLACEMENT_MAP[it] ?: it }
            .take(12)
            .joinToString(separator = "")
    }

    companion object {
        private val REPLACEMENT_MAP = mapOf(
            '0' to 'L',
            '1' to 'Z',
            '2' to 'F',
            '3' to 'p',
            '4' to 'T',
            '5' to 'r',
            '6' to 'S',
            '7' to 'm',
            '8' to 'H',
            '9' to 'x',
        )
    }
}
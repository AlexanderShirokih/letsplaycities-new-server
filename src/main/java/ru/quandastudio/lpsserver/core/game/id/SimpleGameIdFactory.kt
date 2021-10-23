package ru.quandastudio.lpsserver.core.game.id

import org.apache.commons.codec.digest.DigestUtils


class SimpleGameIdFactory : GameIdFactory {
    override fun getBattleHashId(battleId: Long): String {
        return DigestUtils.sha1Hex(battleId.toString()).reversed().map {
            when (it) {
                'a' -> '-'
                'f' -> '+'
                else -> it
            }
        }.joinToString(separator = "")
    }
}
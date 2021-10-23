package ru.quandastudio.lpsserver.core.game.id

interface GameIdFactory {
    fun getBattleHashId(battleId: Long): String
}
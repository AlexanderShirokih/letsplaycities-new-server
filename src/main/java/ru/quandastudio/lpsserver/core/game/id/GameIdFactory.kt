package ru.quandastudio.lpsserver.core.game.id

interface GameIdFactory {
    fun createBattleHash(battleId: Long): String
}
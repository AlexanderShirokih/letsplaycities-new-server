package ru.quandastudio.lpsserver.core.game.id

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

internal class SimpleGameIdFactoryTest {

    @Test
    fun `hash always has fixed length`() {
        val expectedLength = 12
        repeat(10) {
            assertEquals(
                expectedLength,
                SimpleGameIdFactory().createBattleHash(it.toLong()).length,
            )
        }
    }

    @Test
    fun `when id is the same, then hash are always the same`() {
        val id = Random.Default.nextLong()

        val first = SimpleGameIdFactory().createBattleHash(id)

        repeat(5) {
            assertEquals(
                first,
                SimpleGameIdFactory().createBattleHash(id),
            )
        }
    }
}

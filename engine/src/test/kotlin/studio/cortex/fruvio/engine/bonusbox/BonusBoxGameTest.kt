package studio.cortex.fruvio.engine.bonusbox

import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.merge.Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BonusBoxGameTest {
    /** Scripted Rng test-double (no existing pattern to match in `engine/test` — every current
     *  Rng-based test drives a real [studio.cortex.fruvio.engine.merge.SeededRng] by seed instead
     *  of scripting exact draws): replays a fixed queued `nextInt()` sequence so the resulting
     *  [BonusBoxGame.cards] deal is deterministic and known ahead of time by the test. */
    private class ScriptedRng(private val queuedInts: List<Int>) : Rng {
        private var index = 0
        override fun nextDouble(): Double = throw UnsupportedOperationException("not used by BonusBoxGame")
        override fun nextInt(bound: Int): Int {
            val v = queuedInts[index++]
            require(v < bound) { "scripted value $v out of bound $bound" }
            return v
        }
    }

    // FruitTier ordinals: CHERRY=0, RASPBERRY=1, LEMON=2, PEACH=3, WATERMELON=4.
    // Deals: [CHERRY, RASPBERRY, LEMON, PEACH, WATERMELON, CHERRY, RASPBERRY, LEMON, PEACH]
    private val dealtOrdinals = listOf(0, 1, 2, 3, 4, 0, 1, 2, 3)
    private fun newGame() = BonusBoxGame(ScriptedRng(dealtOrdinals))

    @Test fun revealingThreeDistinctCardsSumsOnlyThoseThreeRewards() {
        val game = newGame()
        assertEquals(FruitTier.CHERRY, game.cards[0])
        assertEquals(FruitTier.WATERMELON, game.cards[4])
        assertEquals(FruitTier.PEACH, game.cards[8])

        game.reveal(0) // CHERRY: 5
        game.reveal(4) // WATERMELON: 85
        val events = game.reveal(8) // PEACH: 45

        // Sum of exactly these 3 picks (5 + 85 + 45 = 135) — NOT all 9 dealt cards' rewards.
        assertEquals(135L, game.totalPayout)
        assertTrue(events.any { it == BonusBoxGame.RoundComplete(135L) })
    }

    @Test fun revealingAlreadyRevealedIndexThrows() {
        val game = newGame()
        game.reveal(0)
        assertFailsWith<IllegalArgumentException> { game.reveal(0) }
    }

    @Test fun revealingBeyondPickCountThrows() {
        val game = newGame()
        game.reveal(0)
        game.reveal(1)
        game.reveal(2)
        assertFailsWith<IllegalArgumentException> { game.reveal(3) }
    }

    @Test fun doneAndRoundCompleteFireExactlyOnThirdReveal() {
        val game = newGame()

        val first = game.reveal(0)
        assertTrue(!game.done)
        assertTrue(first.none { it is BonusBoxGame.RoundComplete })

        val second = game.reveal(1)
        assertTrue(!game.done)
        assertTrue(second.none { it is BonusBoxGame.RoundComplete })

        val third = game.reveal(2)
        assertTrue(game.done)
        assertTrue(third.any { it is BonusBoxGame.RoundComplete })
    }
}

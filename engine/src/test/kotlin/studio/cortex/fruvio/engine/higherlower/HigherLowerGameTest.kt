package studio.cortex.fruvio.engine.higherlower

import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.merge.Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Queued/scripted [Rng] test-double: returns a fixed pre-set sequence of `nextInt` results,
 *  ignoring `bound` (this game only ever calls `nextInt(FruitTier.entries.size)`). Fails loudly if
 *  a test under-provisions the queue rather than silently wrapping/cycling. */
private class ScriptedRng(private val values: List<Int>) : Rng {
    private var index = 0
    override fun nextDouble(): Double = throw UnsupportedOperationException("HigherLowerGame never calls nextDouble")
    override fun nextInt(bound: Int): Int {
        check(index < values.size) { "ScriptedRng exhausted after ${values.size} draws" }
        return values[index++]
    }
}

class HigherLowerGameTest {
    // FruitTier ordinals: CHERRY=0, RASPBERRY=1, LEMON=2, PEACH=3, WATERMELON=4.

    @Test fun pushDoesNotConsumeGuessOrChangeStreak() {
        // current draw = CHERRY(0); guess draw ties CHERRY(0) (a push); final draw RASPBERRY(1).
        val game = HigherLowerGame(ScriptedRng(listOf(0, 0, 1)))
        assertEquals(FruitTier.CHERRY, game.current)

        val events = game.guess(HigherLowerGame.Guess.HIGHER)

        assertTrue(events.any { it == HigherLowerGame.Push(FruitTier.CHERRY) })
        assertTrue(events.any { it == HigherLowerGame.Correct(FruitTier.RASPBERRY, 1) })
        assertEquals(1, game.streak)
        assertTrue(game.alive)
    }

    @Test fun correctGuessIncrementsStreakAndReturnsCorrect() {
        val game = HigherLowerGame(ScriptedRng(listOf(0, 1)))
        val events = game.guess(HigherLowerGame.Guess.HIGHER)

        assertEquals(listOf(HigherLowerGame.Correct(FruitTier.RASPBERRY, 1)), events)
        assertEquals(1, game.streak)
        assertTrue(game.alive)
    }

    @Test fun wrongGuessBustsWithNoPayout() {
        // current = LEMON(2); guess HIGHER but draw CHERRY(0) is lower => wrong.
        val game = HigherLowerGame(ScriptedRng(listOf(2, 0)))
        val events = game.guess(HigherLowerGame.Guess.HIGHER)

        assertEquals(listOf(HigherLowerGame.Busted(FruitTier.CHERRY)), events)
        assertTrue(!game.alive)
        assertEquals(0, game.streak)
        assertNull(game.cashOut())
    }

    @Test fun cashOutReturnsNullAtStreakZero() {
        val game = HigherLowerGame(ScriptedRng(listOf(0)))
        assertNull(game.cashOut())
    }

    @Test fun cashOutAtStreakOnePaysExactRuleAmount() {
        val game = HigherLowerGame(ScriptedRng(listOf(0, 1)))
        game.guess(HigherLowerGame.Guess.HIGHER) // CHERRY -> RASPBERRY, streak 1
        val result = game.cashOut()
        assertEquals(HigherLowerGame.CashedOut(1, HigherLowerRules.payout(1)), result)
        assertTrue(!game.alive)
    }

    @Test fun cashOutAtStreakTwoPaysExactRuleAmount() {
        val game = HigherLowerGame(ScriptedRng(listOf(0, 1, 2)))
        game.guess(HigherLowerGame.Guess.HIGHER) // CHERRY -> RASPBERRY, streak 1
        game.guess(HigherLowerGame.Guess.HIGHER) // RASPBERRY -> LEMON, streak 2
        val result = game.cashOut()
        assertEquals(HigherLowerGame.CashedOut(2, HigherLowerRules.payout(2)), result)
    }

    @Test fun cashOutAtStreakThreePaysExactRuleAmount() {
        val game = HigherLowerGame(ScriptedRng(listOf(0, 1, 2, 3)))
        game.guess(HigherLowerGame.Guess.HIGHER) // CHERRY -> RASPBERRY, streak 1
        game.guess(HigherLowerGame.Guess.HIGHER) // RASPBERRY -> LEMON, streak 2
        game.guess(HigherLowerGame.Guess.HIGHER) // LEMON -> PEACH, streak 3
        val result = game.cashOut()
        assertEquals(HigherLowerGame.CashedOut(3, HigherLowerRules.payout(3)), result)
    }

    @Test fun twentyTieChainStillTerminatesGuess() {
        // Every draw (including the initial `current`) returns the same value: a pathological
        // chain of ties. guess() must still return rather than looping forever — the MAX_REDRAWS
        // safety net kicks in at 20 pushes, after which the tied draw is treated as a wrong guess
        // (equal ordinals are neither higher nor lower). 1 constructor draw + 1 initial guess draw
        // + 20 redraw-loop draws = 22 draws consumed; a few extra are provisioned as headroom.
        val game = HigherLowerGame(ScriptedRng(List(30) { 0 }))
        val events = game.guess(HigherLowerGame.Guess.HIGHER)

        assertEquals(20, events.count { it is HigherLowerGame.Push })
        assertTrue(events.last() is HigherLowerGame.Busted)
        assertTrue(!game.alive)
        assertEquals(0, game.streak)
    }
}

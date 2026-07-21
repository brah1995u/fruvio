package studio.cortex.fruvio.engine.merge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CocktailComboTest {
    @Test fun thirdQuickMergeStartsDoubleCocktailBonus() {
        val combo = CocktailCombo()
        combo.recordMerge(); combo.advance(1f)
        combo.recordMerge(); combo.advance(1f)
        val third = combo.recordMerge()
        assertEquals(3, third.streak)
        assertEquals(2, third.multiplier)
        assertEquals(50, third.bonus)
    }

    @Test fun timerExpiryStartsANewStreak() {
        val combo = CocktailCombo()
        combo.recordMerge()
        combo.advance(CocktailCombo.WINDOW_SECONDS)
        assertEquals(1, combo.recordMerge().streak)
    }

    @Test fun freezeStopsDecayForEightSecondsThenResumes() {
        val combo = CocktailCombo()
        combo.recordMerge()
        val before = combo.remainingSeconds
        combo.freeze(CocktailCombo.FREEZE_DURATION_SECONDS)
        combo.advance(7.5f)
        assertEquals(before, combo.remainingSeconds)
        assertTrue(combo.freezeRemainingSeconds > 0f)
        combo.advance(1f)
        assertTrue(combo.remainingSeconds < before)
        assertEquals(0f, combo.freezeRemainingSeconds)
    }
}
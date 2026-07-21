package studio.cortex.fruvio.engine.merge

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WinConditionTest {
    @Test fun reachTierSatisfiedAtOrAboveTarget() {
        val cond = WinCondition.ReachTier(FruitTier.LEMON)
        assertTrue(cond.isSatisfied(MergeState(FruitTier.LEMON, score = 0, dropsUsed = 0)))
        assertTrue(cond.isSatisfied(MergeState(FruitTier.PEACH, score = 0, dropsUsed = 0)))
        assertFalse(cond.isSatisfied(MergeState(FruitTier.RASPBERRY, score = 0, dropsUsed = 0)))
    }

    @Test fun scoreThresholdBoundaryCases() {
        val cond = WinCondition.ScoreThreshold(target = 500)
        assertTrue(cond.isSatisfied(MergeState(FruitTier.CHERRY, score = 500, dropsUsed = 0)))
        assertFalse(cond.isSatisfied(MergeState(FruitTier.CHERRY, score = 499, dropsUsed = 0)))
    }

    @Test fun dropLimitRequiresScoreWithinBudget() {
        val cond = WinCondition.DropLimit(maxDrops = 20, scoreThreshold = 300)
        assertTrue(cond.isSatisfied(MergeState(FruitTier.CHERRY, score = 300, dropsUsed = 20)))
        assertFalse(cond.isSatisfied(MergeState(FruitTier.CHERRY, score = 300, dropsUsed = 21)))
        assertFalse(cond.isSatisfied(MergeState(FruitTier.CHERRY, score = 299, dropsUsed = 20)))
    }
}

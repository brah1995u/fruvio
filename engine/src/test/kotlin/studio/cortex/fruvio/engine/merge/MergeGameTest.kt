package studio.cortex.fruvio.engine.merge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MergeGameTest {
    private val reachTierLevel = LevelDef(
        index = 1, jarWidthUnits = 5, jarHeightUnits = 8,
        spawnTierMin = FruitTier.CHERRY, spawnTierMax = FruitTier.CHERRY,
        winCondition = WinCondition.ReachTier(FruitTier.LEMON), parValue = 6, coinReward = 20,
    )

    @Test fun choreographedPlaythroughReachesTargetTier() {
        val game = MergeGame(reachTierLevel, SeededRng(1))
        repeat(4) { game.dropCurrent() }
        game.reportMerge(FruitTier.RASPBERRY)
        game.reportMerge(FruitTier.RASPBERRY)
        val events = game.reportMerge(FruitTier.LEMON)
        assertTrue(events.any { it == MergeGame.TargetMet })
        assertTrue(game.won)
        assertEquals(4, game.dropsUsed)
    }

    @Test fun realLevel1UsesScoreGoalAndDropBasedStars() {
        val level = Levels.all.first()
        val game = MergeGame(level, SeededRng(99))
        repeat(4) { game.dropCurrent() }
        repeat(4) { game.reportMerge(FruitTier.WATERMELON) }
        assertTrue(game.won)
        assertTrue(game.score >= (level.winCondition as WinCondition.ScoreThreshold).target)
        assertEquals(3, StarRating.stars(level, game.dropsUsed))
    }

    @Test fun comboMultiplierAndBonusAreActuallyAddedToScoreAndBestCombo() {
        val level = Levels.all[3]
        val game = MergeGame(level, SeededRng(7))
        val event = game.reportMerge(FruitTier.LEMON, scoreMultiplier = 2, comboBonus = 50, comboStreak = 3)
            .filterIsInstance<MergeGame.Merged>().single()
        assertEquals(110, event.scoreAwarded)
        assertEquals(110, game.score)
        assertEquals(3, game.bestCombo)
        assertEquals(2, event.comboMultiplier)
        assertEquals(50, event.comboBonus)
    }

    @Test fun comboChallengeNeedsBothStreakAndScore() {
        val level = LevelDef(1, jarWidthUnits = 6, jarHeightUnits = 9,
            spawnTierMin = FruitTier.CHERRY, spawnTierMax = FruitTier.LEMON,
            winCondition = WinCondition.ComboChallenge(3, 100), parValue = 10, coinReward = 20)
        val game = MergeGame(level, SeededRng(8))
        game.reportMerge(FruitTier.WATERMELON, comboStreak = 3)
        assertFalse(game.won)
        val events = game.reportMerge(FruitTier.WATERMELON, comboStreak = 3)
        assertTrue(events.any { it == MergeGame.TargetMet })
    }

    @Test fun terminalStateRejectsFurtherDropsAndMerges() {
        val game = MergeGame(reachTierLevel, SeededRng(9))
        game.reportMerge(FruitTier.LEMON)
        val drops = game.dropsUsed
        val score = game.score
        assertTrue(game.dropCurrent().isEmpty())
        assertTrue(game.reportMerge(FruitTier.WATERMELON).isEmpty())
        assertEquals(drops, game.dropsUsed)
        assertEquals(score, game.score)
    }

    @Test fun dropLimitLosesOnlyAfterBudgetIsExceeded() {
        val level = LevelDef(2, jarWidthUnits = 5, jarHeightUnits = 8,
            spawnTierMin = FruitTier.CHERRY, spawnTierMax = FruitTier.CHERRY,
            winCondition = WinCondition.DropLimit(3, 1000), parValue = 3, coinReward = 20)
        val game = MergeGame(level, SeededRng(2))
        repeat(3) { game.dropCurrent() }
        assertFalse(game.lost)
        assertTrue(game.dropCurrent().any { it == MergeGame.DropLimitExceeded })
    }

    @Test fun overflowGraceTripsResetsAndCanBeExplicitlyCleared() {
        val game = MergeGame(reachTierLevel, SeededRng(3))
        game.tickOverflow(true, 2.5f)
        game.resetOverflowGrace()
        assertTrue(game.tickOverflow(true, 2.9f).isEmpty())
        game.tickOverflow(false, 0.1f)
        assertTrue(game.tickOverflow(true, 3.1f).any { it == MergeGame.Overflowed })
    }

@Test fun tierBoostCanCompleteReachGoalWithoutAwardingScore() {
        val game = MergeGame(reachTierLevel, SeededRng(12))
        val events = game.reportTierBoost(FruitTier.LEMON)
        assertTrue(events.any { it is MergeGame.TierReached })
        assertTrue(events.any { it == MergeGame.TargetMet })
        assertEquals(0, game.score)
        assertTrue(game.won)
    }
}
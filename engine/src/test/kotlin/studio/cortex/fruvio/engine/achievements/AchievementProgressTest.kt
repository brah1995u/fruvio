package studio.cortex.fruvio.engine.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementProgressTest {
    private fun snapshot(
        levelsCount: Int = 5,
        levelsClearedCount: Int = 0,
        levelsWithThreeStarsCount: Int = 0,
        highestTierOrdinalEver: Int = -1,
        lifetimeCoinsEarned: Long = 0L,
        totalMerges: Long = 0L,
        miniGamesPlayedCount: Int = 0,
        miniGameRoundsCount: Int = 0,
        higherLowerBestStreak: Int = 0,
    ) = AchievementSnapshot(
        levelsCount = levelsCount,
        levelsClearedCount = levelsClearedCount,
        levelsWithThreeStarsCount = levelsWithThreeStarsCount,
        highestTierOrdinalEver = highestTierOrdinalEver,
        lifetimeCoinsEarned = lifetimeCoinsEarned,
        totalMerges = totalMerges,
        miniGamesPlayedCount = miniGamesPlayedCount,
        miniGameRoundsCount = miniGameRoundsCount,
        higherLowerBestStreak = higherLowerBestStreak,
    )

    private fun def(metric: AchievementDef.Metric, target: Int) = AchievementDef(
        id = "test_${metric.name.lowercase()}",
        title = "Test",
        description = "Test",
        metric = metric,
        target = target,
        coinReward = 10,
        unitLabel = "levels",
    )

    // ---- LEVELS_CLEARED ----
    @Test fun levelsClearedTracksClearedCountAgainstFixedTarget() {
        val d = def(AchievementDef.Metric.LEVELS_CLEARED, target = 1)
        assertEquals(0, AchievementProgress.currentValue(d, snapshot(levelsClearedCount = 0)))
        assertFalse(AchievementProgress.isComplete(d, snapshot(levelsClearedCount = 0)))
        assertEquals(1, AchievementProgress.currentValue(d, snapshot(levelsClearedCount = 1)))
        assertTrue(AchievementProgress.isComplete(d, snapshot(levelsClearedCount = 1)))
    }

    // ---- ALL_LEVELS_CLEARED: must track snapshot.levelsCount, not def.target ----
    @Test fun allLevelsClearedTracksCurrentLevelsCountAtFive() {
        val d = def(AchievementDef.Metric.ALL_LEVELS_CLEARED, target = 5)
        val s = snapshot(levelsCount = 5, levelsClearedCount = 5)
        assertEquals(5, AchievementProgress.targetValue(d, s))
        assertTrue(AchievementProgress.isComplete(d, s))
        assertFalse(AchievementProgress.isComplete(d, snapshot(levelsCount = 5, levelsClearedCount = 4)))
    }

    @Test fun allLevelsClearedTracksCurrentLevelsCountAtFifty() {
        // def.target is still the stale 5 here — must NOT be used. The real target must follow
        // the snapshot's levelsCount (50), simulating a future content plan growing Levels.all.
        val d = def(AchievementDef.Metric.ALL_LEVELS_CLEARED, target = 5)
        val notYetAllCleared = snapshot(levelsCount = 50, levelsClearedCount = 49)
        assertEquals(50, AchievementProgress.targetValue(d, notYetAllCleared))
        assertFalse(AchievementProgress.isComplete(d, notYetAllCleared))

        val allCleared = snapshot(levelsCount = 50, levelsClearedCount = 50)
        assertTrue(AchievementProgress.isComplete(d, allCleared))
    }

    // ---- ANY_LEVEL_THREE_STARS ----
    @Test fun anyLevelThreeStarsIsBinary() {
        val d = def(AchievementDef.Metric.ANY_LEVEL_THREE_STARS, target = 1)
        assertEquals(0, AchievementProgress.currentValue(d, snapshot(levelsWithThreeStarsCount = 0)))
        assertFalse(AchievementProgress.isComplete(d, snapshot(levelsWithThreeStarsCount = 0)))
        assertEquals(1, AchievementProgress.currentValue(d, snapshot(levelsWithThreeStarsCount = 1)))
        assertTrue(AchievementProgress.isComplete(d, snapshot(levelsWithThreeStarsCount = 3)))
    }

    // ---- ALL_LEVELS_THREE_STARS: must also track snapshot.levelsCount, not def.target ----
    @Test fun allLevelsThreeStarsTracksCurrentLevelsCountAtThree() {
        val d = def(AchievementDef.Metric.ALL_LEVELS_THREE_STARS, target = 5)
        val s = snapshot(levelsCount = 3, levelsWithThreeStarsCount = 3)
        assertEquals(3, AchievementProgress.targetValue(d, s))
        assertTrue(AchievementProgress.isComplete(d, s))
        assertFalse(AchievementProgress.isComplete(d, snapshot(levelsCount = 3, levelsWithThreeStarsCount = 2)))
    }

    @Test fun allLevelsThreeStarsTracksCurrentLevelsCountAtFifty() {
        val d = def(AchievementDef.Metric.ALL_LEVELS_THREE_STARS, target = 5)
        val notYetAllStarred = snapshot(levelsCount = 50, levelsWithThreeStarsCount = 49)
        assertEquals(50, AchievementProgress.targetValue(d, notYetAllStarred))
        assertFalse(AchievementProgress.isComplete(d, notYetAllStarred))

        val allStarred = snapshot(levelsCount = 50, levelsWithThreeStarsCount = 50)
        assertTrue(AchievementProgress.isComplete(d, allStarred))
    }

    // ---- HIGHEST_TIER_ORDINAL ----
    @Test fun highestTierOrdinalCoercesTheNeverMergedSentinelToZero() {
        val d = def(AchievementDef.Metric.HIGHEST_TIER_ORDINAL, target = 4)
        assertEquals(0, AchievementProgress.currentValue(d, snapshot(highestTierOrdinalEver = -1)))
        assertFalse(AchievementProgress.isComplete(d, snapshot(highestTierOrdinalEver = -1)))
        assertEquals(4, AchievementProgress.currentValue(d, snapshot(highestTierOrdinalEver = 4)))
        assertTrue(AchievementProgress.isComplete(d, snapshot(highestTierOrdinalEver = 4)))
    }

    // ---- LIFETIME_COINS_EARNED ----
    @Test fun lifetimeCoinsEarnedTracksLongValue() {
        val d = def(AchievementDef.Metric.LIFETIME_COINS_EARNED, target = 2000)
        assertEquals(1999, AchievementProgress.currentValue(d, snapshot(lifetimeCoinsEarned = 1999L)))
        assertFalse(AchievementProgress.isComplete(d, snapshot(lifetimeCoinsEarned = 1999L)))
        assertEquals(2000, AchievementProgress.currentValue(d, snapshot(lifetimeCoinsEarned = 2000L)))
        assertTrue(AchievementProgress.isComplete(d, snapshot(lifetimeCoinsEarned = 2000L)))
    }

    // ---- TOTAL_MERGES ----
    @Test fun totalMergesTracksLongValue() {
        val d = def(AchievementDef.Metric.TOTAL_MERGES, target = 200)
        assertEquals(199, AchievementProgress.currentValue(d, snapshot(totalMerges = 199L)))
        assertFalse(AchievementProgress.isComplete(d, snapshot(totalMerges = 199L)))
        assertEquals(200, AchievementProgress.currentValue(d, snapshot(totalMerges = 200L)))
        assertTrue(AchievementProgress.isComplete(d, snapshot(totalMerges = 200L)))
    }

    // ---- MINI_GAMES_PLAYED ----
    @Test fun miniGamesPlayedTracksCount() {
        val d = def(AchievementDef.Metric.MINI_GAMES_PLAYED, target = 3)
        assertEquals(2, AchievementProgress.currentValue(d, snapshot(miniGamesPlayedCount = 2)))
        assertFalse(AchievementProgress.isComplete(d, snapshot(miniGamesPlayedCount = 2)))
        assertEquals(3, AchievementProgress.currentValue(d, snapshot(miniGamesPlayedCount = 3)))
        assertTrue(AchievementProgress.isComplete(d, snapshot(miniGamesPlayedCount = 3)))
    }
    @Test fun threeStarLevelsTracksExactCount() {
        val d = def(AchievementDef.Metric.THREE_STAR_LEVELS, target = 5)
        assertFalse(AchievementProgress.isComplete(d, snapshot(levelsWithThreeStarsCount = 4)))
        assertTrue(AchievementProgress.isComplete(d, snapshot(levelsWithThreeStarsCount = 5)))
    }

    @Test fun miniGameRoundsAndHigherLowerStreakTrackPersistentTotals() {
        val rounds = def(AchievementDef.Metric.MINI_GAME_ROUNDS, target = 20)
        val streak = def(AchievementDef.Metric.HIGHER_LOWER_STREAK, target = 5)
        assertEquals(19, AchievementProgress.currentValue(rounds, snapshot(miniGameRoundsCount = 19)))
        assertFalse(AchievementProgress.isComplete(rounds, snapshot(miniGameRoundsCount = 19)))
        assertTrue(AchievementProgress.isComplete(rounds, snapshot(miniGameRoundsCount = 20)))
        assertFalse(AchievementProgress.isComplete(streak, snapshot(higherLowerBestStreak = 4)))
        assertTrue(AchievementProgress.isComplete(streak, snapshot(higherLowerBestStreak = 5)))
    }
}

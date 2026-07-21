package studio.cortex.fruvio.engine.achievements

/** A minimal read-only snapshot of persistent player progress — enough to check any [AchievementDef]. */
data class AchievementSnapshot(
    val levelsCount: Int,
    val levelsClearedCount: Int,
    val levelsWithThreeStarsCount: Int,
    val highestTierOrdinalEver: Int, // -1 sentinel = never merged anything yet
    val lifetimeCoinsEarned: Long,
    val totalMerges: Long,
    val miniGamesPlayedCount: Int, // 0..3
    val miniGameRoundsCount: Int = 0,
    val higherLowerBestStreak: Int = 0,
)

package studio.cortex.fruvio.engine.achievements

/** Pure functions for reading an [AchievementDef]'s progress against an [AchievementSnapshot]. */
object AchievementProgress {
    fun currentValue(def: AchievementDef, s: AchievementSnapshot): Int = when (def.metric) {
        AchievementDef.Metric.LEVELS_CLEARED, AchievementDef.Metric.ALL_LEVELS_CLEARED -> s.levelsClearedCount
        AchievementDef.Metric.ANY_LEVEL_THREE_STARS -> if (s.levelsWithThreeStarsCount > 0) 1 else 0
        AchievementDef.Metric.THREE_STAR_LEVELS -> s.levelsWithThreeStarsCount
        AchievementDef.Metric.ALL_LEVELS_THREE_STARS -> s.levelsWithThreeStarsCount
        AchievementDef.Metric.HIGHEST_TIER_ORDINAL -> s.highestTierOrdinalEver.coerceAtLeast(0)
        AchievementDef.Metric.LIFETIME_COINS_EARNED -> s.lifetimeCoinsEarned.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        AchievementDef.Metric.TOTAL_MERGES -> s.totalMerges.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        AchievementDef.Metric.MINI_GAMES_PLAYED -> s.miniGamesPlayedCount
        AchievementDef.Metric.MINI_GAME_ROUNDS -> s.miniGameRoundsCount
        AchievementDef.Metric.HIGHER_LOWER_STREAK -> s.higherLowerBestStreak
    }

    // IMPORTANT: ALL_LEVELS_CLEARED / ALL_LEVELS_THREE_STARS resolve their target against the
    // snapshot's CURRENT levelsCount, not def.target — Levels.all is expected to grow past 5 in a
    // future content plan, and a hardcoded target here would silently go stale (an achievement that
    // should require "all levels" would stop requiring the new ones). def.target is only a display
    // placeholder for these two metrics, not the real target.
    fun targetValue(def: AchievementDef, s: AchievementSnapshot): Int = when (def.metric) {
        AchievementDef.Metric.ALL_LEVELS_CLEARED, AchievementDef.Metric.ALL_LEVELS_THREE_STARS -> s.levelsCount
        else -> def.target
    }

    fun isComplete(def: AchievementDef, s: AchievementSnapshot): Boolean =
        currentValue(def, s) >= targetValue(def, s)
}

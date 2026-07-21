package studio.cortex.fruvio.engine.achievements

/**
 * One achievement's fixed definition. [id] is a stable pref-key suffix — never rename once
 * shipped, or a player's claimed/unclaimed state silently resets. [target] is ignored for the two
 * ALL_LEVELS_* metrics (see [AchievementProgress.targetValue] for why).
 */
data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val metric: Metric,
    val target: Int,
    val coinReward: Long,
    val unitLabel: String, // "levels" / "coins" / "merges" / "tier" / "mini-games"
) {
    enum class Metric {
        LEVELS_CLEARED, ALL_LEVELS_CLEARED, ANY_LEVEL_THREE_STARS, THREE_STAR_LEVELS, ALL_LEVELS_THREE_STARS,
        HIGHEST_TIER_ORDINAL, LIFETIME_COINS_EARNED, TOTAL_MERGES, MINI_GAMES_PLAYED,
        MINI_GAME_ROUNDS, HIGHER_LOWER_STREAK,
    }
}

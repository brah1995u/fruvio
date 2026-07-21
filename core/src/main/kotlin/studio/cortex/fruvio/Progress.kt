package studio.cortex.fruvio

import com.badlogic.gdx.Preferences
import studio.cortex.fruvio.engine.achievements.AchievementDef
import studio.cortex.fruvio.engine.achievements.AchievementProgress
import studio.cortex.fruvio.engine.achievements.AchievementSnapshot
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.merge.Levels

/** Booster kinds sold for offline fun-coins and consumed inside Merge gameplay. */
enum class Booster(val prefKey: String) {
    EXTRA_PREVIEW("booster_extra_preview"),
    REMOVE_FRUIT("booster_remove_fruit"),
    SHAKE_JAR("booster_shake_jar"),
    COMBO_FREEZE("booster_combo_freeze"),
    REROLL_NEXT("booster_reroll_next"),
    UPGRADE_FRUIT("booster_upgrade_fruit"),
    OVERFLOW_SHIELD("booster_overflow_shield"),
    DOUBLE_REWARD("booster_double_reward"),
}

enum class MiniGame(val prefKey: String) {
    PLINKO("minigame_played_plinko"), BONUS_BOX("minigame_played_bonusbox"), HIGHER_LOWER("minigame_played_higherlower"),
}

class Progress(private val prefs: Preferences) {
    init { migrateLegacyBoosters() }

    var coins: Long
        get() = prefs.getLong("coins", START_COINS)
        set(v) { prefs.putLong("coins", v.coerceAtLeast(0)).flush() }

    fun stars(levelIndex: Int): Int = prefs.getInteger("stars_$levelIndex", 0)

    fun recordStars(levelIndex: Int, starsEarned: Int) {
        val best = maxOf(stars(levelIndex), starsEarned.coerceIn(1, 3))
        prefs.putInteger("stars_$levelIndex", best).flush()
    }

    fun earnCoins(amount: Long) {
        require(amount >= 0)
        coins += amount
        prefs.putLong("lifetime_coins_earned", lifetimeCoinsEarned + amount).flush()
    }

    val lifetimeCoinsEarned: Long get() = prefs.getLong("lifetime_coins_earned", 0L)

    fun boosterCount(b: Booster): Int = prefs.getInteger(b.prefKey, 0).coerceAtLeast(0)

    fun addBooster(b: Booster, qty: Int = 1) {
        require(qty > 0)
        prefs.putInteger(b.prefKey, boosterCount(b) + qty).flush()
    }

    fun consumeBooster(b: Booster): Boolean {
        val count = boosterCount(b)
        if (count <= 0) return false
        prefs.putInteger(b.prefKey, count - 1).flush()
        return true
    }

    private fun migrateLegacyBoosters() {
        if (prefs.getBoolean(BOOSTER_MIGRATION_KEY, false)) return
        val oldUndo = prefs.getInteger("booster_undo_drop", 0).coerceAtLeast(0)
        val oldSlow = prefs.getInteger("booster_slowmo", 0).coerceAtLeast(0)
        if (oldUndo > 0) prefs.putInteger(Booster.REMOVE_FRUIT.prefKey, boosterCount(Booster.REMOVE_FRUIT) + oldUndo)
        if (oldSlow > 0) prefs.putInteger(Booster.SHAKE_JAR.prefKey, boosterCount(Booster.SHAKE_JAR) + oldSlow)
        prefs.remove("booster_undo_drop")
        prefs.remove("booster_slowmo")
        prefs.putBoolean(BOOSTER_MIGRATION_KEY, true).flush()
    }

    fun recordMerges(count: Int = 1) { prefs.putLong("total_merges", totalMerges + count).flush() }
    val totalMerges: Long get() = prefs.getLong("total_merges", 0L)

    fun recordTierReached(tier: FruitTier) {
        prefs.putInteger("highest_tier_ordinal_ever", maxOf(highestTierOrdinalEver, tier.ordinal)).flush()
    }
    val highestTierOrdinalEver: Int get() = prefs.getInteger("highest_tier_ordinal_ever", -1)

    fun markMiniGamePlayed(game: MiniGame) {
        prefs.putBoolean(game.prefKey, true)
        prefs.putInteger("mini_game_rounds", miniGameRoundsCount + 1)
        prefs.flush()
    }
    fun miniGamePlayed(game: MiniGame): Boolean = prefs.getBoolean(game.prefKey, false)
    fun miniGamesPlayedCount(): Int = MiniGame.entries.count { miniGamePlayed(it) }
    val miniGameRoundsCount: Int get() = prefs.getInteger("mini_game_rounds", 0).coerceAtLeast(0)

    var plinkoTickets: Int
        get() = prefs.getInteger("plinko_tickets", 0)
        set(v) { prefs.putInteger("plinko_tickets", v.coerceAtLeast(0)).flush() }

    /** Earned by clearing a main-game level — see [MergeGameScreen.PLINKO_TICKETS_PER_LEVEL]. */
    fun grantPlinkoTickets(qty: Int) {
        require(qty > 0)
        plinkoTickets += qty
    }

    fun recordHigherLowerStreak(streak: Int) {
        prefs.putInteger("hl_best_streak", maxOf(higherLowerBestStreak, streak)).flush()
    }
    val higherLowerBestStreak: Int get() = prefs.getInteger("hl_best_streak", 0)

    fun isAchievementClaimed(id: String): Boolean = prefs.getBoolean("achv_claimed_$id", false)

    fun achievementSnapshot(): AchievementSnapshot = AchievementSnapshot(
        levelsCount = Levels.all.size,
        levelsClearedCount = Levels.all.count { stars(it.index) > 0 },
        levelsWithThreeStarsCount = Levels.all.count { stars(it.index) >= 3 },
        highestTierOrdinalEver = highestTierOrdinalEver,
        lifetimeCoinsEarned = lifetimeCoinsEarned,
        totalMerges = totalMerges,
        miniGamesPlayedCount = miniGamesPlayedCount(),
        miniGameRoundsCount = miniGameRoundsCount,
        higherLowerBestStreak = higherLowerBestStreak,
    )

    fun claimAchievement(def: AchievementDef): Boolean {
        if (isAchievementClaimed(def.id)) return false
        if (!AchievementProgress.isComplete(def, achievementSnapshot())) return false
        prefs.putBoolean("achv_claimed_${def.id}", true).flush()
        earnCoins(def.coinReward)
        return true
    }

    companion object {
        const val START_COINS = 500L
        private const val BOOSTER_MIGRATION_KEY = "booster_migration_v2"
    }
}
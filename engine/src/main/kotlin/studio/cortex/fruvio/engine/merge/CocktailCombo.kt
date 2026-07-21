package studio.cortex.fruvio.engine.merge

/** Timed merge streak for the Fruit Cocktail bonus. Pure and deterministic. */
class CocktailCombo {
    data class Award(val streak: Int, val multiplier: Int, val bonus: Int)

    var streak: Int = 0
        private set
    var remainingSeconds: Float = 0f
        private set
    var freezeRemainingSeconds: Float = 0f
        private set

    fun freeze(durationSeconds: Float) {
        require(durationSeconds > 0f)
        freezeRemainingSeconds = maxOf(freezeRemainingSeconds, durationSeconds)
    }

    fun advance(deltaSeconds: Float) {
        require(deltaSeconds >= 0f)
        var remainingDelta = deltaSeconds
        if (freezeRemainingSeconds > 0f) {
            val frozenPart = minOf(freezeRemainingSeconds, remainingDelta)
            freezeRemainingSeconds -= frozenPart
            remainingDelta -= frozenPart
        }
        if (remainingDelta <= 0f || remainingSeconds <= 0f) return
        remainingSeconds = (remainingSeconds - remainingDelta).coerceAtLeast(0f)
        if (remainingSeconds == 0f) streak = 0
    }

    fun recordMerge(): Award {
        streak = if (remainingSeconds > 0f) streak + 1 else 1
        remainingSeconds = WINDOW_SECONDS
        val multiplier = (1 + streak / 3).coerceAtMost(MAX_MULTIPLIER)
        val bonus = if (streak % 3 == 0) BONUS_PER_TIER * multiplier else 0
        return Award(streak, multiplier, bonus)
    }

    companion object {
        const val WINDOW_SECONDS = 3.25f
        const val MAX_MULTIPLIER = 3
        const val BONUS_PER_TIER = 25
        const val FREEZE_DURATION_SECONDS = 8f
    }
}
package studio.cortex.fruvio.engine.merge

/** Read-only in-progress state used by every win condition. */
data class MergeState(
    val highestTierReached: FruitTier,
    val score: Int,
    val dropsUsed: Int,
    val bestCombo: Int = 0,
)

sealed interface WinCondition {
    fun isSatisfied(state: MergeState): Boolean

    data class ReachTier(val target: FruitTier) : WinCondition {
        override fun isSatisfied(state: MergeState): Boolean = state.highestTierReached >= target
    }

    data class ScoreThreshold(val target: Int) : WinCondition {
        init { require(target > 0) }
        override fun isSatisfied(state: MergeState): Boolean = state.score >= target
    }

    data class DropLimit(val maxDrops: Int, val scoreThreshold: Int) : WinCondition {
        init { require(maxDrops > 0); require(scoreThreshold > 0) }
        override fun isSatisfied(state: MergeState): Boolean =
            state.score >= scoreThreshold && state.dropsUsed <= maxDrops
    }

    /** Reach both a merge streak and a score target in the same level. */
    data class ComboChallenge(val minStreak: Int, val scoreTarget: Int) : WinCondition {
        init { require(minStreak >= 2); require(scoreTarget > 0) }
        override fun isSatisfied(state: MergeState): Boolean =
            state.bestCombo >= minStreak && state.score >= scoreTarget
    }
}
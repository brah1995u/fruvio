package studio.cortex.fruvio.engine.merge

/** Pure-logic half of one physics-driven merge level. */
class MergeGame(
    private val level: LevelDef,
    rng: Rng,
) {
    sealed interface Event
    data class Merged(
        val resultTier: FruitTier,
        val scoreAwarded: Int,
        val comboStreak: Int = 1,
        val comboMultiplier: Int = 1,
        val comboBonus: Int = 0,
    ) : Event
    data class TierReached(val tier: FruitTier) : Event
    object TargetMet : Event
    object DropLimitExceeded : Event
    object Overflowed : Event

    val spawnQueue = SpawnQueue(level.spawnableTiers, rng)

    var score: Int = 0
        private set
    var dropsUsed: Int = 0
        private set
    var bestCombo: Int = 0
        private set
    var highestTierReached: FruitTier = level.spawnableTiers.first()
        private set
    var won: Boolean = false
        private set
    var lost: Boolean = false
        private set

    private fun snapshot() = MergeState(highestTierReached, score, dropsUsed, bestCombo)

    fun dropCurrent(): List<Event> {
        if (won || lost) return emptyList()
        val events = ArrayList<Event>()
        dropsUsed++
        spawnQueue.advance()
        checkDropLimit(events)
        return events
    }

    fun reportMerge(
        resultTier: FruitTier,
        scoreMultiplier: Int = 1,
        comboBonus: Int = 0,
        comboStreak: Int = 1,
    ): List<Event> {
        require(scoreMultiplier >= 1)
        require(comboBonus >= 0)
        require(comboStreak >= 1)
        if (won || lost) return emptyList()

        val events = ArrayList<Event>()
        val baseScore = (resultTier.ordinal + 1) * 10
        val scoreAwarded = baseScore * scoreMultiplier + comboBonus
        score += scoreAwarded
        bestCombo = maxOf(bestCombo, comboStreak)
        events.add(Merged(resultTier, scoreAwarded, comboStreak, scoreMultiplier, comboBonus))

        if (resultTier.ordinal > highestTierReached.ordinal) {
            highestTierReached = resultTier
            events.add(TierReached(resultTier))
        }

        checkWin(events)
        return events
    }

    /** A shop upgrade may advance the tier objective, but never awards merge score or combo. */
    fun reportTierBoost(resultTier: FruitTier): List<Event> {
        if (won || lost || resultTier.ordinal <= highestTierReached.ordinal) return emptyList()
        val events = arrayListOf<Event>(TierReached(resultTier))
        highestTierReached = resultTier
        checkWin(events)
        return events
    }

    private fun checkWin(events: MutableList<Event>) {
        if (!won && !lost && level.winCondition.isSatisfied(snapshot())) {
            won = true
            events.add(TargetMet)
        }
    }

    private fun checkDropLimit(events: MutableList<Event>) {
        val limit = level.winCondition as? WinCondition.DropLimit ?: return
        if (!won && !lost && dropsUsed > limit.maxDrops) {
            lost = true
            events.add(DropLimitExceeded)
        }
    }

    private var overflowTimer = 0f

    fun resetOverflowGrace() { overflowTimer = 0f }

    fun tickOverflow(isOverflowing: Boolean, deltaSeconds: Float): List<Event> {
        val events = ArrayList<Event>()
        if (won || lost) return events
        if (isOverflowing) {
            overflowTimer += deltaSeconds
            if (overflowTimer >= OVERFLOW_GRACE_SECONDS) {
                lost = true
                events.add(Overflowed)
            }
        } else {
            overflowTimer = 0f
        }
        return events
    }

    companion object {
        const val OVERFLOW_GRACE_SECONDS = 3.0f
    }
}
package studio.cortex.fruvio.engine.merge

/**
 * Deterministic queue of upcoming fruit tiers, drawn uniformly from [spawnableTiers] via [rng].
 * [current] is the fruit waiting to be dropped next; [peekNext] looks further ahead without
 * consuming anything (index 0 of the result is the fruit right after [current] — this backs the
 * "Extra Preview" booster's 2-ahead view, design doc §9). Call [advance] once a fruit is dropped.
 */
class SpawnQueue(
    private val spawnableTiers: List<FruitTier>,
    private val rng: Rng,
) {
    init {
        require(spawnableTiers.isNotEmpty()) { "spawnableTiers must not be empty" }
    }

    private val lookahead = ArrayDeque<FruitTier>()

    var current: FruitTier = drawNext()
        private set

    /** Look [count] fruits ahead without consuming them; index 0 is right after [current]. */
    fun peekNext(count: Int = 1): List<FruitTier> {
        while (lookahead.size < count) lookahead.addLast(drawNext())
        return lookahead.take(count)
    }

    /** Consume [current] and pull the next fruit into it (from the lookahead buffer if primed). */
    fun advance(): FruitTier {
        current = if (lookahead.isNotEmpty()) lookahead.removeFirst() else drawNext()
        return current
    }

    /** Replace the waiting fruit without consuming a drop or disturbing the preview buffer. */
    fun rerollCurrent(): FruitTier {
        if (spawnableTiers.size == 1) return current
        val previous = current
        repeat(8) {
            val candidate = drawNext()
            if (candidate != previous) { current = candidate; return current }
        }
        current = spawnableTiers[(spawnableTiers.indexOf(previous) + 1) % spawnableTiers.size]
        return current
    }

    private fun drawNext(): FruitTier = spawnableTiers[rng.nextInt(spawnableTiers.size)]
}

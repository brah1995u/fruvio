package studio.cortex.fruvio.engine.merge

/**
 * Deterministic RNG abstraction. Same seed => same sequence => replayable rounds and
 * reproducible tests. Ported from Flame Jester's `engine/Rng.kt` convention into this module's
 * own package (this plan's scope is `engine/merge` only, not a shared top-level engine package).
 */
interface Rng {
    /** Uniform double in [0, 1). */
    fun nextDouble(): Double
    /** Uniform int in [0, bound). */
    fun nextInt(bound: Int): Int
}

/** Deterministic PRNG backed by Kotlin's cross-platform [kotlin.random.Random]. */
class SeededRng(seed: Long) : Rng {
    private val r = kotlin.random.Random(seed)
    override fun nextDouble(): Double = r.nextDouble()
    override fun nextInt(bound: Int): Int = r.nextInt(bound)
}

package studio.cortex.fruvio.engine.plinko

/**
 * The pure-logic half of one Plinko drop, driven by the (out-of-scope, Box2D-based) physics
 * screen. [resolve] is called once the physics layer identifies which bin the token landed in
 * (either a real contact or the "nearest bin by x" timeout fallback) — mirrors `engine/merge`'s
 * `MergeGame` shape: a small pure class, no Box2D/rendering dependency, one method that returns
 * the event that happened.
 */
class PlinkoRound(
    private val multipliers: List<Double> = PlinkoBoard.BIN_MULTIPLIERS,
    private val cost: Long = PlinkoBoard.PAYOUT_BASE,
) {
    init { require(multipliers.size == PlinkoBoard.BIN_COUNT) }
    sealed interface Event
    data class Landed(val binIndex: Int, val multiplier: Double, val payout: Long) : Event

    var finished = false; private set

    fun resolve(binIndex: Int): Landed {
        require(!finished) { "round already resolved" }
        require(binIndex in multipliers.indices) { "bin index out of range" }
        finished = true
        val mult = multipliers[binIndex]
        return Landed(binIndex, mult, (cost * mult).toLong())
    }
}

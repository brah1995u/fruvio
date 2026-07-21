package studio.cortex.fruvio.engine.plinko

/** Pure payout table shared by Plinko logic, physics and rendering. */
object PlinkoBoard {
    /**
     * Nine narrow pockets. Drops are gated by a scarce per-round ticket (see
     * [studio.cortex.fruvio.Progress.plinkoTickets]) rather than costing coins, so there's no
     * "stake" to lose — every drop is a pure bonus payout, and the curve is deliberately varied
     * (0.7x up to a 3.0x edge) so it reads as a fun grab-bag rather than a flat, predictable rate.
     */
    val BIN_MULTIPLIERS: List<Double> = listOf(3.0, 2.5, 2.0, 1.7, 0.7, 1.7, 2.0, 2.5, 3.0)
    val BIN_WIDTH_WEIGHTS: List<Double> = listOf(0.30, 0.75, 1.05, 1.30, 1.50, 1.30, 1.05, 0.75, 0.30)
    private val totalBinWeight = BIN_WIDTH_WEIGHTS.sum()
    fun binStartFraction(index: Int): Double = BIN_WIDTH_WEIGHTS.take(index).sum() / totalBinWeight
    fun binEndFraction(index: Int): Double = BIN_WIDTH_WEIGHTS.take(index + 1).sum() / totalBinWeight
    const val BIN_COUNT = 9
    /** Scale factor for converting a landed multiplier into a coin payout — not a cost, since
     *  drops are ticket-gated rather than coin-gated. */
    const val PAYOUT_BASE = 20L
}
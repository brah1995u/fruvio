package studio.cortex.fruvio.engine.higherlower

/**
 * Payout table for the Higher-Lower mini-game. Every round starts at [STAKE] coins staked; each
 * correct guess raises the streak by 1 and the multiplier by [STREAK_STEP], up to
 * [MAX_STREAK_FOR_MULTIPLIER] streak (a 7.0x cap) so payouts can't grow unbounded on a long run.
 */
object HigherLowerRules {
    const val STAKE = 30L
    const val STREAK_STEP = 0.5
    const val MAX_STREAK_FOR_MULTIPLIER = 12

    fun multiplier(streak: Int): Double = 1.0 + STREAK_STEP * streak.coerceAtMost(MAX_STREAK_FOR_MULTIPLIER)

    fun payout(streak: Int): Long = (STAKE * multiplier(streak)).toLong()
}

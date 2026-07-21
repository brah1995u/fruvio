package studio.cortex.fruvio.engine.higherlower

import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.merge.Rng

/**
 * Pure-logic Higher-Lower round: guess whether the next drawn [FruitTier] is higher or lower
 * (by [FruitTier.ordinal]) than [current]. A correct guess raises [streak] and can be cashed out
 * at any point via [cashOut] for [HigherLowerRules.payout]; a wrong guess busts the round for
 * nothing. Mirrors [studio.cortex.fruvio.engine.merge.MergeGame]'s shape: methods return exactly
 * the events that happened so a view layer can animate/react without re-deriving state.
 *
 * **Why the tie/push rule exists** (not obvious): [FruitTier] has only 5 distinct values, so a
 * naive next-vs-current draw ties (`next == current`) 20% of the time — too frequent to treat as
 * an auto-loss (would feel unfair to the player) or silently ignore. A tie is a **push**: redraw,
 * doesn't consume the guess or change the streak. [MAX_REDRAWS] is a pathological-chain safety
 * net (vanishingly unlikely to ever trigger with a real RNG, but guarantees [guess] always
 * terminates) — same "always terminates" spirit as `MergeGame`'s overflow-grace-timer pattern,
 * just applied to a redraw loop instead of a wall-clock timer.
 */
class HigherLowerGame(private val rng: Rng) {
    enum class Guess { HIGHER, LOWER }

    sealed interface Event
    data class Push(val redrawnTier: FruitTier) : Event
    data class Correct(val tier: FruitTier, val streak: Int) : Event
    data class Busted(val tier: FruitTier) : Event
    data class CashedOut(val streak: Int, val payout: Long) : Event

    var current: FruitTier = drawTier(); private set
    var streak = 0; private set
    var alive = true; private set

    /** Resolve one guess against a freshly drawn tier. Pushes (ties) redraw silently — see class
     *  doc — until a non-tying tier is found or [MAX_REDRAWS] is hit. */
    fun guess(g: Guess): List<Event> {
        require(alive) { "round already ended" }
        val events = ArrayList<Event>()
        var next = drawTier()
        var redraws = 0
        while (next == current && redraws < MAX_REDRAWS) {
            events += Push(next)
            next = drawTier()
            redraws++
        }
        val correct = if (g == Guess.HIGHER) next.ordinal > current.ordinal else next.ordinal < current.ordinal
        current = next
        if (correct) {
            streak++
            events += Correct(current, streak)
        } else {
            alive = false
            events += Busted(current)
        }
        return events
    }

    /** Locks in the current streak's payout. Returns `null` (no-op) if the round already ended or
     *  nothing has been won yet (streak 0). */
    fun cashOut(): CashedOut? {
        if (!alive || streak < 1) return null
        alive = false
        return CashedOut(streak, HigherLowerRules.payout(streak))
    }

    private fun drawTier(): FruitTier = FruitTier.entries[rng.nextInt(FruitTier.entries.size)]

    private companion object {
        const val MAX_REDRAWS = 20
    }
}

package studio.cortex.fruvio.engine.bonusbox

import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.merge.Rng

/**
 * Pure-logic half of a Bonus Box round: 9 face-down cards are dealt up front (via [rng]), and the
 * player picks [BonusBoxRewards.PICK_COUNT] of them one at a time via [reveal]. Each call returns
 * exactly the events that happened, in order (mirrors `engine/merge/MergeGame`'s shape), so a view
 * layer can animate/react without re-deriving state. The 6 cards never picked are a view-layer-only
 * cosmetic reveal (a cascade) — they never touch [totalPayout], which only ever sums the
 * [BonusBoxRewards.PICK_COUNT] cards actually passed through [reveal].
 */
class BonusBoxGame(rng: Rng) {
    sealed interface Event
    data class Revealed(val cardIndex: Int, val tier: FruitTier, val reward: Long) : Event
    data class RoundComplete(val totalPayout: Long) : Event

    val cards: List<FruitTier> = List(BonusBoxRewards.CARD_COUNT) { FruitTier.entries[rng.nextInt(FruitTier.entries.size)] }

    private val revealedIdx = HashSet<Int>()

    var picksUsed = 0
        private set
    var totalPayout = 0L
        private set

    val done get() = picksUsed >= BonusBoxRewards.PICK_COUNT

    /** Reveals [cardIndex], crediting its reward toward [totalPayout]. Throws if no picks remain
     *  ([done]) or if [cardIndex] is out of range or already revealed. */
    fun reveal(cardIndex: Int): List<Event> {
        require(!done) { "no picks remaining" }
        require(cardIndex in cards.indices && cardIndex !in revealedIdx) { "invalid or already-revealed card" }
        revealedIdx += cardIndex
        picksUsed++
        val tier = cards[cardIndex]
        val reward = BonusBoxRewards.COIN_REWARD.getValue(tier)
        totalPayout += reward
        val events: MutableList<Event> = mutableListOf(Revealed(cardIndex, tier, reward))
        if (done) events += RoundComplete(totalPayout)
        return events
    }
}

package studio.cortex.fruvio.engine.bonusbox

import studio.cortex.fruvio.engine.merge.FruitTier

/**
 * Bonus Box economy. A card averages 34.4 coins, so three picks return about 103 coins against the
 * 90-coin entry cost (roughly 115% RTP). About 59% of ordinary three-card rounds now cover the
 * entry cost: generous enough to keep the mode fun, while peach/watermelon combinations remain
 * the exciting high-value outcome.
 */
object BonusBoxRewards {
    val COIN_REWARD: Map<FruitTier, Long> = mapOf(
        FruitTier.CHERRY to 5L,
        FruitTier.RASPBERRY to 12L,
        FruitTier.LEMON to 25L,
        FruitTier.PEACH to 45L,
        FruitTier.WATERMELON to 85L,
    )

    const val CARD_COUNT = 9
    const val PICK_COUNT = 3
    const val PLAY_COST = 90L
}
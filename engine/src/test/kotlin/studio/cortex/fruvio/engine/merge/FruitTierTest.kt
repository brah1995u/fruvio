package studio.cortex.fruvio.engine.merge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FruitTierTest {
    @Test fun fiveTiersInOrder() {
        assertEquals(5, FruitTier.entries.size)
        assertEquals(FruitTier.CHERRY, FruitTier.entries.first())
        assertEquals(FruitTier.WATERMELON, FruitTier.entries.last())
    }

    @Test fun nextTierWalksUpTheOrder() {
        assertEquals(FruitTier.RASPBERRY, FruitTier.CHERRY.next)
        assertEquals(FruitTier.PEACH, FruitTier.LEMON.next)
    }

    @Test fun topTierHasNoNext() {
        assertNull(FruitTier.WATERMELON.next)
    }
}

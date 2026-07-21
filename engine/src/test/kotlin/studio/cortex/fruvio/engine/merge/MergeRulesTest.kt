package studio.cortex.fruvio.engine.merge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MergeRulesTest {
    @Test fun equalTiersMergeIntoNextTierUp() {
        assertEquals(FruitTier.RASPBERRY, MergeRules.resolve(FruitTier.CHERRY, FruitTier.CHERRY))
        assertEquals(FruitTier.PEACH, MergeRules.resolve(FruitTier.LEMON, FruitTier.LEMON))
    }

    @Test fun topTierMergingWithItselfHasNoTarget() {
        assertNull(MergeRules.resolve(FruitTier.WATERMELON, FruitTier.WATERMELON))
    }

    @Test fun unequalTiersDoNotMerge() {
        assertNull(MergeRules.resolve(FruitTier.CHERRY, FruitTier.RASPBERRY))
        assertNull(MergeRules.resolve(FruitTier.LEMON, FruitTier.CHERRY))
    }
}

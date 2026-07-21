package studio.cortex.fruvio.engine.merge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LevelDefTest {
    private fun baseLevel(
        spawnTierMin: FruitTier = FruitTier.CHERRY,
        spawnTierMax: FruitTier = FruitTier.RASPBERRY,
        jarWidthUnits: Int = 6,
        jarHeightUnits: Int = 9,
        parValue: Int = 10,
        // Defaults to the top tier so tests that vary spawnTierMax for unrelated reasons don't
        // accidentally trip the ReachTier-target invariant below.
        reachTarget: FruitTier = FruitTier.WATERMELON,
    ) = LevelDef(
        index = 1,
        jarWidthUnits = jarWidthUnits,
        jarHeightUnits = jarHeightUnits,
        spawnTierMin = spawnTierMin,
        spawnTierMax = spawnTierMax,
        winCondition = WinCondition.ReachTier(reachTarget),
        parValue = parValue,
        coinReward = 10,
    )

    @Test fun spawnTierMinAboveMaxIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            baseLevel(spawnTierMin = FruitTier.LEMON, spawnTierMax = FruitTier.CHERRY)
        }
    }

    @Test fun nonPositiveJarDimensionsAreRejected() {
        assertFailsWith<IllegalArgumentException> { baseLevel(jarWidthUnits = 0) }
        assertFailsWith<IllegalArgumentException> { baseLevel(jarHeightUnits = -1) }
    }

    @Test fun nonPositiveParValueIsRejected() {
        assertFailsWith<IllegalArgumentException> { baseLevel(parValue = 0) }
    }

    @Test fun spawnableTiersIsInclusiveRange() {
        val level = baseLevel(spawnTierMin = FruitTier.CHERRY, spawnTierMax = FruitTier.LEMON)
        assertEquals(
            listOf(FruitTier.CHERRY, FruitTier.RASPBERRY, FruitTier.LEMON),
            level.spawnableTiers,
        )
    }

    @Test fun reachTierTargetAtSpawnTierMaxIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            baseLevel(spawnTierMax = FruitTier.LEMON, reachTarget = FruitTier.LEMON)
        }
    }

    @Test fun reachTierTargetBelowSpawnTierMaxIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            baseLevel(spawnTierMax = FruitTier.PEACH, reachTarget = FruitTier.LEMON)
        }
    }

    @Test fun reachTierTargetAboveSpawnTierMaxIsAccepted() {
        val level = baseLevel(spawnTierMax = FruitTier.LEMON, reachTarget = FruitTier.PEACH)
        assertEquals(FruitTier.PEACH, (level.winCondition as WinCondition.ReachTier).target)
    }
}

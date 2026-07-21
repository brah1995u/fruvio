package studio.cortex.fruvio.engine.merge

import kotlin.test.Test
import kotlin.test.assertEquals

class StarRatingTest {
    private val level = LevelDef(1, jarWidthUnits = 6, jarHeightUnits = 9,
        spawnTierMin = FruitTier.CHERRY, spawnTierMax = FruitTier.LEMON,
        winCondition = WinCondition.ScoreThreshold(300), parValue = 10, coinReward = 50)

    @Test fun allMissionTypesUseDropCountAgainstPar() {
        assertEquals(3, StarRating.stars(level, 8))
        assertEquals(3, StarRating.stars(level, 10))
        assertEquals(2, StarRating.stars(level, 11))
        assertEquals(2, StarRating.stars(level, 15))
        assertEquals(1, StarRating.stars(level, 16))
    }

    @Test fun oddParUsesCorrectIntegerDropBoundary() {
        val odd = level.copy(parValue = 9)
        assertEquals(2, StarRating.stars(odd, 13))
        assertEquals(1, StarRating.stars(odd, 14))
    }
}
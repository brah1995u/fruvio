package studio.cortex.fruvio.engine.plinko

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlinkoRoundTest {
    @Test fun payoutMatchesMultiplierForEveryBin() {
        for (binIndex in PlinkoBoard.BIN_MULTIPLIERS.indices) {
            val round = PlinkoRound()
            val landed = round.resolve(binIndex)
            val expectedPayout = (PlinkoBoard.PAYOUT_BASE * PlinkoBoard.BIN_MULTIPLIERS[binIndex]).toLong()
            assertEquals(expectedPayout, landed.payout)
            assertEquals(PlinkoBoard.BIN_MULTIPLIERS[binIndex], landed.multiplier)
            assertEquals(binIndex, landed.binIndex)
        }
    }

    @Test fun outOfRangeBinIndexThrows() {
        val round = PlinkoRound()
        assertFailsWith<IllegalArgumentException> { round.resolve(-1) }
        assertFailsWith<IllegalArgumentException> { round.resolve(PlinkoBoard.BIN_COUNT) }
    }

    @Test fun resolvingTwiceThrows() {
        val round = PlinkoRound()
        round.resolve(0)
        assertFailsWith<IllegalArgumentException> { round.resolve(1) }
    }

    @Test fun customCostScalesPayout() {
        val round = PlinkoRound(cost = 50L)
        val landed = round.resolve(2) // multiplier 2.0
        assertEquals(100L, landed.payout)
    }
    @Test fun payoutCurveIsVariedWithNoStakeToLose() {
        assertEquals(9, PlinkoBoard.BIN_COUNT)
        // Tickets are the scarce resource, not a coin stake, so only the center pocket dips
        // below 1.0x — every drop is still a net gain, just a smaller or larger one.
        assertEquals(1, PlinkoBoard.BIN_MULTIPLIERS.count { it < 1.0 })
        assertEquals(0.7, PlinkoBoard.BIN_MULTIPLIERS[PlinkoBoard.BIN_COUNT / 2])
        assertEquals(3.0, PlinkoBoard.BIN_MULTIPLIERS.first())
        assertTrue(PlinkoBoard.BIN_MULTIPLIERS.min() >= 0.7)
        assertEquals(PlinkoBoard.BIN_MULTIPLIERS, PlinkoBoard.BIN_MULTIPLIERS.reversed())
        assertEquals(PlinkoBoard.BIN_WIDTH_WEIGHTS, PlinkoBoard.BIN_WIDTH_WEIGHTS.reversed())
    }

@Test fun roundUsesTheDisplayedPerRoundMultiplierLayout() {
        val layout = listOf(3.2, 1.3, 0.25, 0.75, 0.55, 0.75, 0.25, 1.3, 3.2)
        val landed = PlinkoRound(multipliers = layout).resolve(3)
        assertEquals(0.75, landed.multiplier)
        assertEquals(15L, landed.payout)
    }
}
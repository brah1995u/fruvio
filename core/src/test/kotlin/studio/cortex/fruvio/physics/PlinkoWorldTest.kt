package studio.cortex.fruvio.physics

import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.plinko.PlinkoBoard
import kotlin.test.Test
import kotlin.test.assertTrue

class PlinkoWorldTest {
    private fun stepUntilLanded(w: PlinkoWorld): PlinkoWorld.BinBodyData? {
        val maxSteps = (PlinkoWorld.MAX_SIMULATION_SECONDS / (1f / 60f)).toInt()
        repeat(maxSteps) {
            w.step(1f / 60f)
            w.drainPendingLanding()?.let { return it }
        }
        return null
    }

    @Test fun tokenDroppedFromAboveEventuallyLandsInSomeBin() {
        val w = PlinkoWorld()
        // Deliberately off-center (not board-center exactly): a perfectly centered x lines up
        // with a peg column on the offset rows, a degenerate unstable-equilibrium case a real
        // drag-release input essentially never produces exactly.
        w.spawnToken(FruitTier.CHERRY, x = PlinkoWorld.BOARD_WIDTH_UNITS / 2f + 0.15f, y = PlinkoWorld.BOARD_HEIGHT_UNITS - 0.5f)
        val landed = stepUntilLanded(w)
        assertTrue(landed != null, "expected the token to contact some bin floor within MAX_SIMULATION_SECONDS")
        w.dispose()
    }

    @Test fun tokenDroppedNearLeftEdgeLandsInLowBinIndex() {
        val w = PlinkoWorld()
        w.spawnToken(FruitTier.CHERRY, x = 0.5f, y = PlinkoWorld.BOARD_HEIGHT_UNITS - 0.5f)
        val landed = stepUntilLanded(w)
        assertTrue(landed != null)
        assertTrue(landed!!.index <= 2, "expected a low bin index near 0, got ${landed.index}")
        w.dispose()
    }

    @Test fun variableWidthBinMappingCoversEdgesAndCenter() {
        val world = PlinkoWorld()
        assertTrue(world.binIndexForX(0f) == 0)
        assertTrue(world.binIndexForX(PlinkoWorld.BOARD_WIDTH_UNITS / 2f) == PlinkoBoard.BIN_COUNT / 2)
        assertTrue(world.binIndexForX(PlinkoWorld.BOARD_WIDTH_UNITS) == PlinkoBoard.BIN_COUNT - 1)
        world.dispose()
    }
    @Test fun wallsKeepTokenInsideBoardBoundsThroughout() {
        val w = PlinkoWorld()
        // Spawned half-overlapping the right wall on purpose, below the peg field (in the bin
        // clearance band) so this isolates wall push-back from any peg interaction — without a
        // working wall, nothing would push it back (gravity alone has no horizontal component).
        val body = w.spawnToken(FruitTier.CHERRY, x = PlinkoWorld.BOARD_WIDTH_UNITS - PlinkoWorld.TOKEN_RADIUS * 0.5f, y = 1.0f)
        repeat(60) { w.step(1f / 60f) }
        assertTrue(
            body.position.x <= PlinkoWorld.BOARD_WIDTH_UNITS - PlinkoWorld.TOKEN_RADIUS + 0.05f,
            "expected the right wall to push the token back inside the board, got x=${body.position.x}",
        )
        assertTrue(body.position.x >= 0f, "token should never be pushed past the left wall either")
        w.dispose()
    }
    @Test fun deterministicDropSampleIsVariedAndMostlyAboveBreakEven() {
        val world = PlinkoWorld()
        var multiplierTotal = 0.0
        var losses = 0
        val counts = IntArray(PlinkoBoard.BIN_COUNT)
        var timeouts = 0
        val rounds = 72
        repeat(rounds) { index ->
            val lane = (index * 37) % 101
            val x = PlinkoWorld.DROP_MIN_X +
                lane / 100f * (PlinkoWorld.DROP_MAX_X - PlinkoWorld.DROP_MIN_X)
            world.spawnToken(FruitTier.entries[index % FruitTier.entries.size], x,
                PlinkoWorld.BOARD_HEIGHT_UNITS - PlinkoWorld.TOKEN_RADIUS)
            val landed = stepUntilLanded(world)
            if (landed == null) timeouts++
            val binIndex = landed?.index ?: world.binIndexForX(world.liveTokenBody()!!.position.x)
            counts[binIndex]++
            val multiplier = PlinkoBoard.BIN_MULTIPLIERS[binIndex]
            multiplierTotal += multiplier
            if (multiplier < 1.0) losses++
        }
        val realizedReturn = multiplierTotal / rounds
        assertTrue(realizedReturn in 1.60..2.10, "expected a varied, well-above-break-even return, got $realizedReturn; bins=${counts.toList()}")
        assertTrue(losses < rounds / 2, "most drops should land at 1.0x or better, got $losses/$rounds below 1.0x")
        assertTrue(timeouts <= 2, "too many stuck fruits: $timeouts/$rounds")
        world.dispose()
    }
}

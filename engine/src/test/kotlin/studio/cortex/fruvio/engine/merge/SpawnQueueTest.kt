package studio.cortex.fruvio.engine.merge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpawnQueueTest {
    private val tiers = listOf(FruitTier.CHERRY, FruitTier.RASPBERRY, FruitTier.LEMON)

    private fun drawSequence(seed: Long, count: Int): List<FruitTier> {
        val queue = SpawnQueue(tiers, SeededRng(seed))
        val out = ArrayList<FruitTier>()
        out.add(queue.current)
        repeat(count - 1) { out.add(queue.advance()) }
        return out
    }

    @Test fun sameSeedProducesSameSequence() {
        assertEquals(drawSequence(seed = 42, count = 10), drawSequence(seed = 42, count = 10))
    }

    @Test fun differentSeedsCanProduceDifferentSequences() {
        assertTrue(drawSequence(seed = 1, count = 20) != drawSequence(seed = 2, count = 20))
    }

    @Test fun allProducedTiersAreWithinTheAllowedCap() {
        val sequence = drawSequence(seed = 7, count = 50)
        assertTrue(sequence.all { it in tiers })
    }

    @Test fun peekNextDoesNotConsumeCurrentAndMatchesSubsequentAdvances() {
        val queue = SpawnQueue(tiers, SeededRng(seed = 5))
        val currentBefore = queue.current
        val preview = queue.peekNext(2)
        assertEquals(currentBefore, queue.current, "peekNext must not consume current")
        assertEquals(preview[0], queue.advance())
        assertEquals(preview[1], queue.advance())
    }

@Test fun rerollChangesCurrentWithoutConsumingPreview() {
        val queue = SpawnQueue(tiers, SeededRng(seed = 11))
        val previous = queue.current
        val preview = queue.peekNext(2)
        val rerolled = queue.rerollCurrent()
        assertTrue(rerolled != previous)
        assertEquals(preview[0], queue.advance())
        assertEquals(preview[1], queue.advance())
    }
}
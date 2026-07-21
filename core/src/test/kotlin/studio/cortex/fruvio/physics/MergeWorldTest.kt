package studio.cortex.fruvio.physics

import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.merge.Levels
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MergeWorldTest {
    private val level = Levels.all.first()

    @Test fun fruitFallsAndWallsKeepItInsideJar() {
        val world = MergeWorld(level)
        val radius = world.radiusMeters(FruitTier.CHERRY)
        val body = world.spawnFruit(FruitTier.CHERRY, level.jarWidthUnits - radius * 0.5f, 6f)
        repeat(180) { world.step(1f / 60f) }
        assertTrue(body.position.y in (radius - 0.05f)..(radius + 0.3f))
        assertTrue(body.position.x <= level.jarWidthUnits - radius + 0.05f)
        world.dispose()
    }

    @Test fun sameTierContactsMergeButDifferentTiersDoNot() {
        val world = MergeWorld(level)
        val r = world.radiusMeters(FruitTier.CHERRY)
        world.spawnFruit(FruitTier.CHERRY, 2.5f - r * 0.9f, 1f)
        world.spawnFruit(FruitTier.CHERRY, 2.5f + r * 0.9f, 1f)
        world.step(1f / 60f)
        assertTrue(world.drainPendingMerges().isNotEmpty())
        assertTrue(world.drainPendingMerges().isEmpty())
        world.dispose()

        val mixed = MergeWorld(level)
        mixed.spawnFruit(FruitTier.CHERRY, 2.5f - r * 0.9f, 1f)
        mixed.spawnFruit(FruitTier.RASPBERRY, 2.5f + r * 0.9f, 1f)
        mixed.step(1f / 60f)
        assertTrue(mixed.drainPendingMerges().isEmpty())
        mixed.dispose()
    }

    @Test fun overflowIgnoresFreshFruitThenUsesTopEdgeAfterGrace() {
        val world = MergeWorld(level)
        val y = level.jarHeightUnits * 0.95f
        val body = world.spawnFruit(FruitTier.CHERRY, 1f, y)
        assertFalse(world.isOverflowing())
        repeat(60) {
            world.step(1f / 60f)
            body.setTransform(1f, y, 0f)
            body.setLinearVelocity(0f, 0f)
        }
        assertTrue(world.isOverflowing())
        world.dispose()
    }

    @Test fun removeFruitUsesHitTestAndConsumesTheBodyOnlyOnce() {
        val world = MergeWorld(level)
        val body = world.spawnFruit(FruitTier.LEMON, 2f, 2f)
        assertNotNull(world.findFruitAt(2f, 2f))
        world.removeFruit(body)
        assertTrue(world.liveFruitBodies().isEmpty())
        assertTrue(world.findFruitAt(2f, 2f) == null)
        world.removeFruit(body)
        world.dispose()
    }

    @Test fun shakeAppliesControlledVelocityWithoutDestroyingBodies() {
        val world = MergeWorld(level)
        val bodies = listOf(
            world.spawnFruit(FruitTier.CHERRY, 2f, 2f),
            world.spawnFruit(FruitTier.LEMON, 4f, 2f),
        )
        world.shake()
        assertTrue(bodies.all { it.linearVelocity.len2() > 0f })
        repeat(90) { world.step(1f / 60f) }
        assertTrue(bodies.all { it.position.x in 0f..level.jarWidthUnits.toFloat() })
        world.dispose()
    }
}
package studio.cortex.fruvio.physics

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.World
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Proves Box2D genuinely works headlessly in this project's plain-JUnit `:core:test` setup.
 * Before this test + its `testImplementation` dependency (see this plan's Task 2), `core`'s main
 * classpath had `gdx-box2d` but no native library at all for tests — any JVM test that touched a
 * Box2D `World` would fail with `UnsatisfiedLinkError`. If this test ever fails for a reason
 * beyond a missing/misconfigured native dependency (e.g. a `Gdx.app`-null NPE inside Box2D's own
 * internals), that is a real escalation point, not something to work around silently.
 */
class Box2DSpikeTest {
    @Test fun bodyFallsUnderGravityInAHeadlessWorld() {
        Box2D.init()
        val world = World(Vector2(0f, -20f), true)
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(0f, 10f)
        }
        val body = world.createBody(bodyDef)
        val shape = CircleShape().apply { radius = 0.5f }
        body.createFixture(FixtureDef().apply { this.shape = shape; density = 1f })
        shape.dispose()

        val startY = body.position.y
        repeat(30) { world.step(1f / 60f, 6, 2) }
        val endY = body.position.y

        assertTrue(endY < startY, "expected body to fall under gravity: started at $startY, ended at $endY")
        world.dispose()
    }
}

package studio.cortex.fruvio.physics

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.World
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.merge.LevelDef

/** Box2D jar plus gameplay-safe fruit operations used by boosters. */
class MergeWorld(private val level: LevelDef) {
    data class FruitBodyData(val tier: FruitTier, val id: Long, var ageSeconds: Float = 0f)

    val world: World = World(Vector2(0f, GRAVITY_Y), true)

    private val liveBodies = ArrayList<Body>()
    private val pendingMerges = ArrayList<Pair<Body, Body>>()
    private var nextId = 0L
    private var disposed = false

    init {
        Box2D.init()
        createWalls()
        world.setContactListener(object : ContactListener {
            override fun beginContact(contact: Contact) {
                val dataA = contact.fixtureA.body.userData as? FruitBodyData ?: return
                val dataB = contact.fixtureB.body.userData as? FruitBodyData ?: return
                if (dataA.tier == dataB.tier) pendingMerges.add(contact.fixtureA.body to contact.fixtureB.body)
            }
            override fun endContact(contact: Contact) {}
            override fun preSolve(contact: Contact, oldManifold: Manifold) {}
            override fun postSolve(contact: Contact, impulse: ContactImpulse) {}
        })
    }

    fun radiusMeters(tier: FruitTier): Float = BASE_RADIUS + tier.ordinal * RADIUS_STEP

    fun spawnFruit(tier: FruitTier, x: Float, y: Float): Body {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
        }
        val body = world.createBody(bodyDef)
        val shape = CircleShape().apply { radius = radiusMeters(tier) }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = FRUIT_DENSITY
            friction = FRUIT_FRICTION
            restitution = FRUIT_RESTITUTION
        }
        body.createFixture(fixtureDef)
        shape.dispose()
        body.userData = FruitBodyData(tier, nextId++)
        liveBodies.add(body)
        return body
    }

    fun removeFruit(body: Body) {
        if (!liveBodies.remove(body)) return
        pendingMerges.removeAll { it.first === body || it.second === body }
        world.destroyBody(body)
    }

    /** Return the closest fruit whose circle contains the supplied world-space point. */
    fun findFruitAt(x: Float, y: Float): Body? = liveBodies
        .asSequence()
        .map { body ->
            val data = body.userData as FruitBodyData
            val dx = body.position.x - x
            val dy = body.position.y - y
            val distance2 = dx * dx + dy * dy
            Triple(body, distance2, radiusMeters(data.tier) * HIT_RADIUS_SCALE)
        }
        .filter { (_, distance2, radius) -> distance2 <= radius * radius }
        .minByOrNull { (_, distance2, _) -> distance2 }
        ?.first

    /** Deterministic alternating impulses keep the shake useful without throwing fruit through walls. */
    fun shake() {
        liveBodies.forEach { body ->
            val data = body.userData as FruitBodyData
            val direction = if (data.id % 2L == 0L) -1f else 1f
            val horizontal = direction * (SHAKE_HORIZONTAL + (data.id % 3L) * 0.18f)
            val vertical = SHAKE_VERTICAL + (data.id % 4L) * 0.12f
            body.applyLinearImpulse(horizontal, vertical, body.worldCenter.x, body.worldCenter.y, true)
        }
    }

    fun step(delta: Float) {
        world.step(delta, VELOCITY_ITERATIONS, POSITION_ITERATIONS)
        liveBodies.forEach { (it.userData as FruitBodyData).ageSeconds += delta }
    }

    fun drainPendingMerges(): List<Pair<Body, Body>> {
        val copy = pendingMerges.toList()
        pendingMerges.clear()
        return copy
    }

    /** Ignore freshly dropped fruit and compare the fruit's top edge with the visible danger line. */
    fun isOverflowing(): Boolean {
        val threshold = OVERFLOW_FRACTION * level.jarHeightUnits
        return liveBodies.any { body ->
            val data = body.userData as FruitBodyData
            data.ageSeconds >= SPAWN_OVERFLOW_GRACE_SECONDS &&
                body.position.y + radiusMeters(data.tier) > threshold
        }
    }

    fun liveFruitBodies(): List<Body> = liveBodies

    fun dispose() {
        if (disposed) return
        disposed = true
        world.dispose()
    }

    private fun createWalls() {
        val w = level.jarWidthUnits.toFloat()
        val h = level.jarHeightUnits.toFloat()
        wallBody(w / 2f, -WALL_THICKNESS / 2f, w / 2f, WALL_THICKNESS / 2f)
        wallBody(-WALL_THICKNESS / 2f, h / 2f, WALL_THICKNESS / 2f, h / 2f)
        wallBody(w + WALL_THICKNESS / 2f, h / 2f, WALL_THICKNESS / 2f, h / 2f)
    }

    private fun wallBody(cx: Float, cy: Float, halfW: Float, halfH: Float) {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.StaticBody
            position.set(cx, cy)
        }
        val body = world.createBody(bodyDef)
        val shape = PolygonShape().apply { setAsBox(halfW, halfH) }
        body.createFixture(shape, 0f)
        shape.dispose()
    }

    companion object {
        const val GRAVITY_Y = -20f
        const val BASE_RADIUS = 0.32f
        const val RADIUS_STEP = 0.14f
        const val FRUIT_DENSITY = 1f
        const val FRUIT_FRICTION = 0.4f
        const val FRUIT_RESTITUTION = 0.15f
        const val WALL_THICKNESS = 0.2f
        const val OVERFLOW_FRACTION = 0.92f
        const val SPAWN_OVERFLOW_GRACE_SECONDS = 0.75f
        const val VELOCITY_ITERATIONS = 6
        const val POSITION_ITERATIONS = 2
        const val HIT_RADIUS_SCALE = 1.2f
        const val SHAKE_HORIZONTAL = 1.35f
        const val SHAKE_VERTICAL = 2.2f
    }
}
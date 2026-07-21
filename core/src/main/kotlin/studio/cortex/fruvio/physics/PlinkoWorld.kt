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
import studio.cortex.fruvio.engine.plinko.PlinkoBoard
import kotlin.math.sin

/** Denser Galton board with narrow physical pockets and a real Figma fruit as its token. */
class PlinkoWorld {
    object PegBodyData
    object DividerBodyData
    data class BinBodyData(val index: Int, val multiplier: Double)
    data class TokenBodyData(val id: Long, val tier: FruitTier)

    val world: World = World(Vector2(0f, GRAVITY_Y), true)
    private var liveToken: Body? = null
    private var pendingLanding: BinBodyData? = null
    private var elapsedSeconds = 0f
    private var nextId = 0L
    private var disposed = false

    init {
        Box2D.init()
        createWalls()
        createPegs()
        createBins()
        world.setContactListener(object : ContactListener {
            override fun beginContact(contact: Contact) {
                val bodyA = contact.fixtureA.body
                val bodyB = contact.fixtureB.body
                val dataA = bodyA.userData
                val dataB = bodyB.userData
                val tokenBody = when {
                    dataA is TokenBodyData -> bodyA
                    dataB is TokenBodyData -> bodyB
                    else -> null
                }
                val pegBody = when {
                    dataA === PegBodyData -> bodyA
                    dataB === PegBodyData -> bodyB
                    else -> null
                }
                if (tokenBody != null && pegBody != null) {
                    val tokenData = tokenBody.userData as TokenBodyData
                    val dx = tokenBody.position.x - pegBody.position.x
                    val direction = if (kotlin.math.abs(dx) > 0.025f) kotlin.math.sign(dx)
                        else if (tokenData.id % 2L == 0L) -1f else 1f
                    tokenBody.applyLinearImpulse(direction * PEG_CONTACT_NUDGE, 0f,
                        tokenBody.worldCenter.x, tokenBody.worldCenter.y, true)
                }
                val bin = (dataA as? BinBodyData) ?: (dataB as? BinBodyData) ?: return
                if (tokenBody != null && pendingLanding == null) pendingLanding = bin
            }
            override fun endContact(contact: Contact) = Unit
            override fun preSolve(contact: Contact, oldManifold: Manifold) = Unit
            override fun postSolve(contact: Contact, impulse: ContactImpulse) = Unit
        })
    }

    fun spawnToken(tier: FruitTier, x: Float, y: Float): Body {
        liveToken?.let { world.destroyBody(it) }
        val body = world.createBody(BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
            bullet = true
        })
        val shape = CircleShape().apply { radius = TOKEN_RADIUS }
        body.createFixture(FixtureDef().apply {
            this.shape = shape
            density = TOKEN_DENSITY
            friction = TOKEN_FRICTION
            restitution = TOKEN_RESTITUTION
        })
        shape.dispose()
        val id = nextId++
        body.userData = TokenBodyData(id, tier)
        // A tiny deterministic nudge removes perfectly symmetric, easily exploitable paths while
        // remaining small enough that the player's chosen drop position still matters.
        val jitter = sin((id + 1.0) * 12.9898 + x * 2.417).toFloat() * SPAWN_JITTER_IMPULSE
        body.applyLinearImpulse(jitter, 0f, body.worldCenter.x, body.worldCenter.y, true)
        liveToken = body
        pendingLanding = null
        elapsedSeconds = 0f
        return body
    }

    fun liveTokenBody(): Body? = liveToken

    fun step(delta: Float) {
        world.step(delta, VELOCITY_ITERATIONS, POSITION_ITERATIONS)
        elapsedSeconds += delta
        val token = liveToken
        token?.let { keepInsideSideWalls(it) }
        if (pendingLanding == null && token != null && token.position.y <= BIN_CAPTURE_Y) {
            val index = binIndexForX(token.position.x)
            pendingLanding = BinBodyData(index, PlinkoBoard.BIN_MULTIPLIERS[index])
        }
    }

    /** Box2D's polygon skin can leave a fast circle fractionally embedded in an outer wall.
     * Keep the token centre inside the authored board without affecting normal peg motion. */
    private fun keepInsideSideWalls(token: Body) {
        val minX = TOKEN_RADIUS
        val maxX = BOARD_WIDTH_UNITS - TOKEN_RADIUS
        val clampedX = token.position.x.coerceIn(minX, maxX)
        if (clampedX == token.position.x) return
        token.setTransform(clampedX, token.position.y, token.angle)
        val velocity = token.linearVelocity
        val reflectedX = when {
            clampedX == minX && velocity.x < 0f -> -velocity.x * WALL_BOUNCE
            clampedX == maxX && velocity.x > 0f -> -velocity.x * WALL_BOUNCE
            else -> velocity.x
        }
        token.setLinearVelocity(reflectedX, velocity.y)
    }

    fun isTimedOut(): Boolean = elapsedSeconds >= MAX_SIMULATION_SECONDS

    fun drainPendingLanding(): BinBodyData? {
        val result = pendingLanding
        pendingLanding = null
        return result
    }

    fun binIndexForX(x: Float): Int {
        val fraction = (x / BOARD_WIDTH_UNITS).coerceIn(0f, 1f)
        return (0 until PlinkoBoard.BIN_COUNT).firstOrNull {
            fraction <= PlinkoBoard.binEndFraction(it)
        } ?: PlinkoBoard.BIN_COUNT - 1
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        world.dispose()
    }

    private fun createWalls() {
        val h = BOARD_HEIGHT_UNITS
        boxBody(-WALL_THICKNESS / 2f, h / 2f, WALL_THICKNESS / 2f, h / 2f, null)
        boxBody(BOARD_WIDTH_UNITS + WALL_THICKNESS / 2f, h / 2f, WALL_THICKNESS / 2f, h / 2f, null)
    }

    private fun createPegs() {
        val topY = BOARD_HEIGHT_UNITS - PEG_TOP_MARGIN
        val centerX = BOARD_WIDTH_UNITS / 2f
        val halfSpan = (PEG_COLUMNS - 1) * COLUMN_SPACING / 2f
        repeat(PEG_ROWS) { row ->
            val y = topY - row * ROW_SPACING
            val offset = if (row % 2 == 0) ROW_OFFSET else 0f
            repeat(PEG_COLUMNS) { column ->
                val x = centerX - halfSpan + column * COLUMN_SPACING + offset
                val body = world.createBody(BodyDef().apply {
                    type = BodyDef.BodyType.StaticBody
                    position.set(x, y)
                })
                val shape = CircleShape().apply { radius = PEG_RADIUS }
                body.createFixture(FixtureDef().apply { this.shape = shape; restitution = PEG_RESTITUTION })
                shape.dispose()
                body.userData = PegBodyData
            }
        }
    }

    private fun createBins() {
        repeat(PlinkoBoard.BIN_COUNT) { index ->
            val startX = BOARD_WIDTH_UNITS * PlinkoBoard.binStartFraction(index).toFloat()
            val endX = BOARD_WIDTH_UNITS * PlinkoBoard.binEndFraction(index).toFloat()
            val width = endX - startX
            boxBody((startX + endX) / 2f, BIN_FLOOR_HEIGHT / 2f, width / 2f, BIN_FLOOR_HEIGHT / 2f,
                BinBodyData(index, PlinkoBoard.BIN_MULTIPLIERS[index]))
            if (index > 0) {
                boxBody(startX, BIN_FLOOR_HEIGHT + BIN_DIVIDER_HEIGHT / 2f,
                    BIN_DIVIDER_WIDTH / 2f, BIN_DIVIDER_HEIGHT / 2f, DividerBodyData)
            }
        }
    }

    private fun boxBody(cx: Float, cy: Float, halfWidth: Float, halfHeight: Float, data: Any?) {
        val body = world.createBody(BodyDef().apply {
            type = BodyDef.BodyType.StaticBody
            position.set(cx, cy)
        })
        val shape = PolygonShape().apply { setAsBox(halfWidth, halfHeight) }
        body.createFixture(shape, 0f)
        shape.dispose()
        body.userData = data
    }

    companion object {
        const val GRAVITY_Y = -28f
        const val PEG_RADIUS = 0.13f
        const val TOKEN_RADIUS = 0.24f
        const val BOARD_WIDTH_UNITS = 10f
        const val DROP_MIN_X = 2.35f
        const val DROP_MAX_X = BOARD_WIDTH_UNITS - DROP_MIN_X
        const val PEG_ROWS = 11
        const val ROW_SPACING = 0.90f
        const val COLUMN_SPACING = 0.95f
        const val ROW_OFFSET = COLUMN_SPACING / 2f
        const val PEG_COLUMNS = 9
        const val TOKEN_RESTITUTION = 0.35f
        const val PEG_RESTITUTION = 0.42f
        const val MAX_SIMULATION_SECONDS = 7f
        const val TOKEN_DENSITY = 0.92f
        const val TOKEN_FRICTION = 0.06f
        const val WALL_THICKNESS = 0.2f
        const val BIN_DIVIDER_WIDTH = 0.08f
        const val BIN_DIVIDER_HEIGHT = 0.82f
        const val SPAWN_JITTER_IMPULSE = 0.14f
        const val PEG_CONTACT_NUDGE = 0.055f
        const val VELOCITY_ITERATIONS = 8
        const val POSITION_ITERATIONS = 6
        const val WALL_BOUNCE = 0.35f
        const val PEG_TOP_MARGIN = 0.92f
        const val BIN_CLEARANCE = 0.92f
        const val BIN_FLOOR_HEIGHT = 0.42f
        const val BIN_CAPTURE_Y = BIN_FLOOR_HEIGHT + BIN_DIVIDER_HEIGHT + TOKEN_RADIUS * 1.1f
        val BOARD_HEIGHT_UNITS = PEG_TOP_MARGIN + (PEG_ROWS - 1) * ROW_SPACING + BIN_CLEARANCE + BIN_FLOOR_HEIGHT
    }
}
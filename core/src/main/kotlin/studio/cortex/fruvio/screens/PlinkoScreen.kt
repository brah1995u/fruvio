package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.MiniGame
import studio.cortex.fruvio.Sfx
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.anim.easeOutCubic
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.plinko.PlinkoBoard
import studio.cortex.fruvio.engine.plinko.PlinkoRound
import studio.cortex.fruvio.physics.PlinkoWorld
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.ActionRole
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.Dialog
import studio.cortex.fruvio.ui.Ui
import java.util.Locale
import kotlin.random.Random

/** Fruit Plinko: a ticket-gated bonus round (see [studio.cortex.fruvio.Progress.plinkoTickets]) —
 *  narrow pockets, dense peg field, no coin cost, varied 0.7x-3.0x payout per drop. */
class PlinkoScreen(game: FruvioGame) : BaseScreen(game) {
    private val plinkoWorld = PlinkoWorld()
    private var plinkoRound: PlinkoRound? = null
    private var physicsAccumulator = 0f

    private val pixelsPerMeter = run {
        val maxW = Theme.W - PLAY_AREA_SIDE_MARGIN * 2f - BOARD_FRAME_PAD * 2f
        val maxH = HUD_BOTTOM_Y - PLAY_AREA_BOTTOM - BOARD_FRAME_PAD * 2f
        minOf(maxW / PlinkoWorld.BOARD_WIDTH_UNITS, maxH / PlinkoWorld.BOARD_HEIGHT_UNITS)
    }
    private val boardWidthPx = PlinkoWorld.BOARD_WIDTH_UNITS * pixelsPerMeter
    private val boardHeightPx = PlinkoWorld.BOARD_HEIGHT_UNITS * pixelsPerMeter
    private val boardOriginX = (Theme.W - boardWidthPx) / 2f
    private val boardOriginY = PLAY_AREA_BOTTOM + BOARD_FRAME_PAD +
        ((HUD_BOTTOM_Y - PLAY_AREA_BOTTOM - BOARD_FRAME_PAD * 2f - boardHeightPx) / 2f).coerceAtLeast(0f)
    private val dropScreenY = boardOriginY + boardHeightPx - PlinkoWorld.TOKEN_RADIUS * pixelsPerMeter

    private var dropX = PlinkoWorld.BOARD_WIDTH_UNITS / 2f
    private var dragging = false
    private var roundActive = false
    private var nextFruit = FruitTier.entries.random()
    private var currentMultipliers = randomizedMultipliers()

    private data class SpawnPunch(var elapsed: Float)
    private val spawnPunches = HashMap<Long, SpawnPunch>()

    private val backBtn = addBackNavigation { game.toMiniGames() }
    private val homeBtn = addHomeNavigation()

    override fun update(delta: Float) {
        if (!roundActive) return
        physicsAccumulator += delta.coerceIn(0f, MAX_FRAME_DELTA)
        var steps = 0
        while (physicsAccumulator >= FIXED_STEP && steps < MAX_STEPS_PER_FRAME && roundActive) {
            plinkoWorld.step(FIXED_STEP)
            physicsAccumulator -= FIXED_STEP
            steps++
            val landing = plinkoWorld.drainPendingLanding()
            if (landing != null) {
                resolveRound(landing.index)
            } else if (plinkoWorld.isTimedOut()) {
                val x = plinkoWorld.liveTokenBody()?.position?.x ?: PlinkoWorld.BOARD_WIDTH_UNITS / 2f
                resolveRound(plinkoWorld.binIndexForX(x))
            }
        }
        if (steps == MAX_STEPS_PER_FRAME) physicsAccumulator = physicsAccumulator.coerceAtMost(FIXED_STEP)
    }

    private fun resolveRound(binIndex: Int) {
        val round = plinkoRound ?: return
        roundActive = false
        val landed = round.resolve(binIndex)
        val won = landed.multiplier >= 1.0
        if (!won && landed.payout > 0) game.progress.earnCoins(landed.payout)
        game.progress.markMiniGamePlayed(MiniGame.PLINKO)
        game.audio.play(if (won) Sfx.WIN else Sfx.LOSE)
        game.buzz(if (won) 60 else 30)
        openLandingDialog(landed)
    }

    // Ticket-gated, not coin-gated (see dropFruit) — every drop is a pure bonus, so there's no
    // stake to net against. The dialog just shows what you won.
    private fun openLandingDialog(landed: PlinkoRound.Landed) {
        val multiplier = formatMultiplier(landed.multiplier)
        val won = landed.multiplier >= 1.0
        val d = Dialog(760f, 800f, "LANDED ${multiplier}x", onDismiss = {
            if (won) game.toMiniGames() else {
                closeDialog()
                prepareNextRound()
            }
        })
        d.fruitRain = won
        d.celebration = won
        d.defeat = !won
        d.drawContent = { batch, assets, px, py, pw, ph ->
            // Dialog skips its own title while celebration is on, so the multiplier — the whole point
            // of the round — would otherwise vanish behind the "WOW! / YOU WIN!" banner. Re-add it as
            // a caption inside the content area, and drop the content lower on the taller win panel.
            Ui.fitText(batch, assets, "LANDED ${multiplier}x", px + pw / 2f,
                py + ph - (if (won) 1_000f else 630f), pw - 160f, 40f, Color.WHITE, font = assets.captionLight)
            Ui.fitText(batch, assets, "PAYOUT +${landed.payout}", px + pw / 2f,
                py + ph - (if (won) 1_085f else 710f), pw - 140f, 48f, Color.WHITE)
        }
        // The loss branch keeps the original compact panel (760x800), so it must keep the original
        // button rows too — the celebration rows are positioned for the taller 860x1080 panel and
        // would sit below a non-celebration panel's bottom edge.
        if (!won) d.buttons += Button(d.cx - 280f, d.cy - 80f, 560f, 130f, "DROP AGAIN", onClick = {
            closeDialog()
            prepareNextRound()
        })
        d.buttons += if (won) {
            Button(d.cx - 350f, d.cy - 480f, 700f, 176f, "COLLECT ALL\nREWARDS!", onClick = {
                if (landed.payout > 0) game.progress.earnCoins(landed.payout)
                game.toMiniGames()
            })
        } else {
            Button(d.cx - 280f, d.cy - 240f, 560f, 130f, "BACK TO MINI GAMES", onClick = { game.toMiniGames() })
        }
        openDialog(d)
    }

    internal fun debugShowWinDialog() =
        openLandingDialog(PlinkoRound.Landed(binIndex = 3, multiplier = 2.0, payout = 40L))
    internal fun debugShowLossDialog() =
        openLandingDialog(PlinkoRound.Landed(binIndex = 0, multiplier = 0.5, payout = 0L))

    override fun onTouchDown(x: Float, y: Float): Boolean {
        if (roundActive || y > HUD_BOTTOM_Y || y < PLAY_AREA_BOTTOM) return false
        dragging = true
        updateDropX(x)
        return true
    }

    override fun onTouchDragged(x: Float, y: Float): Boolean {
        if (roundActive || !dragging) return false
        updateDropX(x)
        return true
    }

    override fun onTouchUp(x: Float, y: Float): Boolean {
        if (roundActive || !dragging) return false
        dragging = false
        dropFruit()
        return true
    }

    private fun updateDropX(screenX: Float) {
        val worldX = (screenX - boardOriginX) / pixelsPerMeter
        dropX = worldX.coerceIn(PlinkoWorld.DROP_MIN_X, PlinkoWorld.DROP_MAX_X)
    }

    private fun dropFruit() {
        if (roundActive || game.progress.plinkoTickets <= 0) return
        game.progress.plinkoTickets -= 1
        val droppedTier = nextFruit
        val body = plinkoWorld.spawnToken(droppedTier, dropX,
            PlinkoWorld.BOARD_HEIGHT_UNITS - PlinkoWorld.TOKEN_RADIUS)
        spawnPunches[(body.userData as PlinkoWorld.TokenBodyData).id] = SpawnPunch(0f)
        nextFruit = FruitTier.entries[Random.nextInt(FruitTier.entries.size)]
        game.audio.play(Sfx.DROP)
        game.buzz(15)
        physicsAccumulator = 0f
        roundActive = true
        plinkoRound = PlinkoRound(multipliers = currentMultipliers)
    }

    private fun prepareNextRound() {
        plinkoRound = null
        currentMultipliers = randomizedMultipliers()
        dropX = PlinkoWorld.BOARD_WIDTH_UNITS / 2f
    }

    /** Pins the outer edge pair from [PlinkoBoard.BIN_MULTIPLIERS] and reshuffles the inner five,
     *  mirroring the symmetric layout without hardcoding a second copy of the payout table. */
    private fun randomizedMultipliers(): List<Double> {
        val edge = PlinkoBoard.BIN_MULTIPLIERS.take(2)
        val center = PlinkoBoard.BIN_MULTIPLIERS.subList(2, PlinkoBoard.BIN_MULTIPLIERS.size - 2).shuffled(Random)
        return edge + center + edge.reversed()
    }

    internal fun debugSeedDemo() {
        dropX = PlinkoWorld.BOARD_WIDTH_UNITS / 2f + 0.17f
        dropFruit()
    }

    override fun draw(delta: Float) {
        tickEffects(delta)
        Draw.cover(batch, a.fruitBackground(7))
        drawHud()
        drawBoard()
        drawPegs()
        drawBins()
        drawFruit()
        drawDropPreview()
        drawNavigation(backBtn, homeBtn)
    }

    private fun tickEffects(delta: Float) {
        val expired = ArrayList<Long>()
        for ((id, punch) in spawnPunches) {
            punch.elapsed += delta
            if (punch.elapsed >= DROP_PUNCH_DURATION) expired += id
        }
        expired.forEach { spawnPunches.remove(it) }
    }

    private fun drawHud() {
        val hasTickets = game.progress.plinkoTickets > 0
        Ui.coinCounter(batch, a, Theme.W - 320f, 2304f, "%,d".format(game.progress.coins), badgeSize = 90f, maxTextW = 180f)
        Ui.fitText(batch, a, "TICKETS x${game.progress.plinkoTickets}", Theme.W - 700f, 2304f, 260f, 60f,
            if (hasTickets) Color.WHITE else Theme.peachOrange)
        Draw.textCentered(batch, a.title, "FRUIT PLINKO", Theme.W / 2f, 2190f, Color.WHITE)
        // No feedback existed when a drop silently no-ops at 0 tickets — swap the usual how-to-play
        // line for a pointer to where tickets actually come from (a level win, see
        // MergeGameScreen.PLINKO_TICKETS_PER_LEVEL), instead of leaving the player guessing.
        val subtitle = if (hasTickets) "DRAG WIDE · MULTIPLIERS SHUFFLE EACH DROP" else "OUT OF TICKETS · WIN A LEVEL TO EARN MORE"
        Ui.fitText(batch, a, subtitle, Theme.W / 2f, 2100f, 940f, 34f, font = a.small,
            color = if (hasTickets) Color.WHITE else Theme.peachOrange)
    }

    private fun drawBoard() {
        Draw.panel(batch, a.uiSkin.jarPanel,
            boardOriginX - BOARD_FRAME_PAD, boardOriginY - BOARD_FRAME_PAD,
            boardWidthPx + BOARD_FRAME_PAD * 2f, boardHeightPx + BOARD_FRAME_PAD * 2f)
    }

    private fun toScreenX(worldX: Float) = boardOriginX + worldX * pixelsPerMeter
    private fun toScreenY(worldY: Float) = boardOriginY + worldY * pixelsPerMeter

    private fun drawPegs() {
        val physicsDiameter = PlinkoWorld.PEG_RADIUS * 2f * pixelsPerMeter
        val visualSize = physicsDiameter * PEG_VISUAL_SCALE
        val topY = PlinkoWorld.BOARD_HEIGHT_UNITS - PlinkoWorld.PEG_TOP_MARGIN
        val centerX = PlinkoWorld.BOARD_WIDTH_UNITS / 2f
        val halfSpan = (PlinkoWorld.PEG_COLUMNS - 1) * PlinkoWorld.COLUMN_SPACING / 2f
        repeat(PlinkoWorld.PEG_ROWS) { row ->
            val y = topY - row * PlinkoWorld.ROW_SPACING
            val offset = if (row % 2 == 0) PlinkoWorld.ROW_OFFSET else 0f
            repeat(PlinkoWorld.PEG_COLUMNS) { column ->
                val x = centerX - halfSpan + column * PlinkoWorld.COLUMN_SPACING + offset
                val sx = toScreenX(x)
                val sy = toScreenY(y)
                val tier = FruitTier.entries[(row + column) % FruitTier.entries.size]
                Draw.imageFit(batch, a.region(tier), sx, sy, visualSize, visualSize)
            }
        }
    }

    private fun drawBins() {
        batch.setColor(Theme.lemonYellow)
        for (divider in 1 until PlinkoBoard.BIN_COUNT) {
            val dividerX = PlinkoWorld.BOARD_WIDTH_UNITS * PlinkoBoard.binStartFraction(divider).toFloat()
            batch.draw(a.white, toScreenX(dividerX) - DIVIDER_DRAW_WIDTH / 2f,
                toScreenY(PlinkoWorld.BIN_FLOOR_HEIGHT), DIVIDER_DRAW_WIDTH,
                PlinkoWorld.BIN_DIVIDER_HEIGHT * pixelsPerMeter)
        }
        batch.setColor(Color.WHITE)
        repeat(PlinkoBoard.BIN_COUNT) { index ->
            val startX = PlinkoWorld.BOARD_WIDTH_UNITS * PlinkoBoard.binStartFraction(index).toFloat()
            val endX = PlinkoWorld.BOARD_WIDTH_UNITS * PlinkoBoard.binEndFraction(index).toFloat()
            val centerX = (startX + endX) / 2f
            val pocketWidthPx = (endX - startX) * pixelsPerMeter
            val multiplier = currentMultipliers[index]
            val screenX = toScreenX(centerX)
            val screenY = toScreenY(PlinkoWorld.BIN_FLOOR_HEIGHT + 0.33f)
            val color = if (multiplier > 1.0) Theme.lemonYellow else Theme.peachOrange
            Ui.fitText(batch, a, "${formatMultiplier(multiplier)}x", screenX, screenY,
                pocketWidthPx * 0.88f, 42f, color)
        }
    }
    private fun drawFruit() {
        val body = plinkoWorld.liveTokenBody() ?: return
        val data = body.userData as PlinkoWorld.TokenBodyData
        var diameter = PlinkoWorld.TOKEN_RADIUS * 2f * pixelsPerMeter * TOKEN_VISUAL_SCALE
        spawnPunches[data.id]?.let { punch ->
            diameter *= easeOutCubic((punch.elapsed / DROP_PUNCH_DURATION).coerceIn(0f, 1f))
        }
        Draw.imageFitRotated(batch, a.region(data.tier), toScreenX(body.position.x), toScreenY(body.position.y),
            diameter, diameter, body.angle * com.badlogic.gdx.math.MathUtils.radiansToDegrees)
    }

    private fun drawDropPreview() {
        if (roundActive) return
        val diameter = PlinkoWorld.TOKEN_RADIUS * 2f * pixelsPerMeter * TOKEN_VISUAL_SCALE
        Draw.imageFit(batch, a.region(nextFruit), toScreenX(dropX), dropScreenY, diameter, diameter, alpha = 0.9f)
    }

    private fun formatMultiplier(value: Double): String = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

    override fun hide() { plinkoWorld.dispose() }
    override fun dispose() { plinkoWorld.dispose() }

    private companion object {
        const val FIXED_STEP = 1f / 60f
        const val MAX_FRAME_DELTA = 0.10f
        const val MAX_STEPS_PER_FRAME = 5
        const val HUD_BOTTOM_Y = 2050f
        const val PLAY_AREA_BOTTOM = 250f
        const val PLAY_AREA_SIDE_MARGIN = 60f
        const val BOARD_FRAME_PAD = 26f
        const val TOKEN_VISUAL_SCALE = 1.45f
        const val PEG_VISUAL_SCALE = 1.48f
        const val DIVIDER_DRAW_WIDTH = 6f
        const val DROP_PUNCH_DURATION = 0.12f
    }
}
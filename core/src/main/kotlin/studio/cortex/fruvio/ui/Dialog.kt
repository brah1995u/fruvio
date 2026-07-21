package studio.cortex.fruvio.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import studio.cortex.fruvio.Assets
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.anim.easeOutBack
import studio.cortex.fruvio.anim.easeOutCubic
import studio.cortex.fruvio.anim.easeOutQuint
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.render.Draw
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Asset-backed modal with fitted titles and optional fruit celebration rain. */
open class Dialog(
    var w: Float, var h: Float,
    var title: String? = null,
    var onDismiss: (() -> Unit)? = null,
) {
    val buttons = ArrayList<Button>()
    private var closeBtn: Button? = null
    var visible = false
        private set
    var anim = 0f
        private set
    var drawContent: ((SpriteBatch, Assets, Float, Float, Float, Float) -> Unit)? = null
    var fruitRain: Boolean = false
    private var rainTime = 0f

    /** Opt-in "grandiose" win treatment — fireworks, confetti, a starburst-lit prize fruit, a
     *  coin-stack cluster, and a "WOW! / YOU WIN!" banner overlapping the panel's top edge, all from
     *  existing atlas assets (no new art). Off by default so every other Dialog in the game — and the
     *  loss-outcome branches of the same win functions — renders exactly as before. Setting it also
     *  forces the panel to a fixed larger size so callers don't need to separately edit their
     *  `Dialog(w, h, ...)` constructor literal. */
    var celebration: Boolean = false
        set(value) {
            field = value
            if (value) { w = CELEBRATION_W; h = CELEBRATION_H }
        }
    /** Large but deliberately quiet counterpart to [celebration]. It keeps the distinct
     * victory-window frame for every failure state, while using an opaque dark backdrop and only
     * a small, slow fruit drift so losing is clear rather than visually rewarding. */
    var defeat: Boolean = false
        set(value) {
            field = value
            if (value) { w = DEFEAT_W; h = DEFEAT_H }
        }
    private var celebrationTime = 0f

    val cx get() = Theme.W / 2f
    val cy get() = Theme.H / 2f

    fun show() { visible = true; anim = 0f; rainTime = 0f; celebrationTime = 0f }
    fun hide() { visible = false }
    fun dismiss() {
        val action = onDismiss
        if (action != null) action() else hide()
    }

    open fun tick(delta: Float) {
        if (visible && anim < 1f) anim = (anim + delta / POP_DURATION).coerceAtMost(1f)
        if (visible && fruitRain) rainTime += delta
        if (visible && (celebration || defeat)) celebrationTime += delta
        for (b in buttons) Ui.tick(b, delta)
    }

    open fun draw(batch: SpriteBatch, a: Assets) {
        if (!visible) return
        val p = anim.coerceIn(0f, 1f)
        val specialOutcome = celebration || defeat
        if (specialOutcome) Draw.cover(batch, a.bgWater)
        val backdropDim = when {
            celebration -> 0.58f
            defeat -> 0.78f
            else -> 0.66f
        }
        batch.setColor(0f, 0f, 0f, backdropDim * p)
        batch.draw(a.white, 0f, 0f, Theme.W, Theme.H)
        batch.setColor(Color.WHITE)
        if (celebration) drawGodRays(batch, a, p)
        if (celebration) drawFireworks(batch, a, p)
        if (celebration) drawConfetti(batch, a, p)
        if (defeat) drawDefeatDrift(batch, a, p)
        if (fruitRain) drawFruitRain(batch, a, p)

        val scale = 0.7f + 0.3f * easeOutBack(p)
        val pw = w * scale; val ph = h * scale
        val px = cx - pw / 2f; val py = cy - ph / 2f
        if (specialOutcome) drawCelebrationFrame(batch, a, px, py, pw, ph)
        else Draw.panel(batch, a.uiSkin.jarPanel, px, py, pw, ph)

        if (!specialOutcome) {
            val close = closeBtn ?: Button(
                0f, 0f, CLOSE_BTN_SIZE, CLOSE_BTN_SIZE, style = Button.Style.SECONDARY,
                region = a.iconClose, role = ActionRole.CLOSE, onClick = { dismiss() },
            ).also { closeBtn = it; buttons += it }
            close.x = px + pw - CLOSE_BTN_SIZE - CLOSE_BTN_MARGIN
            close.y = py + ph - CLOSE_BTN_SIZE - CLOSE_BTN_MARGIN
        }
        if (p > 0.55f) {
            if (celebration) {
                val prizeY = py + ph - 575f
                drawStarburstAndPrize(batch, a, cx, prizeY)
                drawPrizeAccents(batch, a, cx, prizeY)
            }
            if (defeat) drawDefeatFruit(batch, a, cx, py + ph - 590f)
            if (!specialOutcome) {
                title?.let { Ui.fitText(batch, a, it, cx, py + ph - 110f, pw - 230f, 104f, Color.WHITE) }
            }
            drawContent?.invoke(batch, a, px, py, pw, ph)
            if (celebration) {
                drawCoinClusters(batch, a, cx, py + 440f)
                drawBanner(batch, a, cx, py + ph, scale)
                layoutCelebrationButtons()
            }
            if (defeat) {
                drawDefeatBanner(batch, a, cx, py + ph, scale)
                layoutDefeatButtons()
            }
            for (b in buttons) Ui.button(batch, a, b)
            if (celebration) {
                drawButtonShimmer(batch, a)
                drawSparkles(batch, a, px, py, pw, ph)
            }
        }
    }

    private fun drawFruitRain(batch: SpriteBatch, a: Assets, alpha: Float) {
        repeat(RAIN_FRUIT_COUNT) { index ->
            val distance = Theme.H + 420f
            val speed = 310f + (index % 5) * 38f
            val y = Theme.H + 110f - ((rainTime * speed + index * 173f) % distance)
            var x = 54f + ((index * 211) % 972)
            val size = 58f + (index % 4) * 14f
            // Keep live fruit decorations clear of the large prize fruit in every victory dialog.
            if (celebration && abs(y - CELEBRATION_PRIZE_Y) < PRIZE_CLEARANCE_Y &&
                abs(x - Theme.W / 2f) < PRIZE_CLEARANCE_X) {
                x = if (x < Theme.W / 2f) Theme.W / 2f - PRIZE_CLEARANCE_X else Theme.W / 2f + PRIZE_CLEARANCE_X
            }
            val tier = FruitTier.entries[index % FruitTier.entries.size]
            batch.setColor(1f, 1f, 1f, 0.84f * alpha)
            Draw.imageFitRotated(batch, a.region(tier), x, y, size, size,
                rainTime * (42f + index) + index * 31f)
        }
        batch.setColor(Color.WHITE)
    }

    /**
     * The celebration popup's own frame. An earlier version deliberately avoided [UiSkin.jarPanel]
     * (the gameplay jar's own surface) and built a shell from an unrelated warm-red panel plus a
     * blue panel tinted violet — two different nine-patches with different bevel/rim proportions
     * never read as one frame, just as three mismatched nested rectangles. Layering [UiSkin.jarPanel]
     * on itself (untinted, so its own gold rim stays gold) instead gives one consistent gold-on-navy
     * frame with a genuinely thicker rim — on-brand with every other panel in the game — and the
     * dimmed backdrop plus the fireworks/confetti/prize content already make it unmistakably a modal,
     * not "the board".
     */
    private fun drawCelebrationFrame(batch: SpriteBatch, a: Assets, px: Float, py: Float, pw: Float, ph: Float) {
        Draw.imageFit(batch, a.victoryFrame, px + pw / 2f, py + ph / 2f, pw, ph)
    }

    /** Rotating fan of warm light rays behind everything — the reference design's most striking
     *  element. Uses [Draw.rectRotated] rather than the aspect-preserving fit helpers because the
     *  only flat source is a square 4x4 [Assets.white]; a ray has to be long and thin, which the
     *  fit helpers physically cannot express. Alternating wide/narrow rays at two alpha levels reads
     *  as a light burst rather than a uniform pinwheel. */
    private fun drawGodRays(batch: SpriteBatch, a: Assets, alpha: Float) {
        val originX = Theme.W / 2f
        val originY = Theme.H / 2f
        val spin = celebrationTime * 5f
        // Triangle-wave breathing so the whole fan swells and settles rather than strobing.
        val pulse = 0.6f + 0.4f * (1f - abs(1f - 2f * ((celebrationTime * 0.3f) % 1f)))
        repeat(GOD_RAY_COUNT) { index ->
            val angleDeg = index * (360f / GOD_RAY_COUNT) + spin
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val wide = index % 2 == 0
            val rayW = if (wide) 130f else 66f
            val rayAlpha = (if (wide) 0.14f else 0.08f) * pulse * alpha
            Draw.rectRotated(
                batch, a.white,
                originX + (GOD_RAY_LENGTH / 2f) * cos(angleRad).toFloat(),
                originY + (GOD_RAY_LENGTH / 2f) * sin(angleRad).toFloat(),
                rayW, GOD_RAY_LENGTH, angleDeg - 90f, Theme.lemonYellow, rayAlpha,
            )
        }
    }

    /** 4 corner sparkle bursts — no streak/trail asset exists in the atlas, so this is a pulsing
     *  radial burst of tinted [Assets.iconStar] copies per corner, not a literal firework trail.
     *  Sized up and kept brighter for longer (first fixed attempt rendered as barely-visible tiny
     *  dots — too small/too faded to read as a "burst" against the dim backdrop). */
    private fun drawFireworks(batch: SpriteBatch, a: Assets, alpha: Float) {
        val corners = listOf(190f to 2040f, 890f to 2040f, 190f to 360f, 890f to 360f)
        val tints = listOf(Theme.cherryRed, Theme.lemonYellow, Theme.peachOrange, Theme.raspberryMaroon, Theme.watermelonGreen)
        corners.forEachIndexed { corner, (cornerX, cornerY) ->
            repeat(FIREWORK_STREAKS_PER_CORNER) { k ->
                val phase = (celebrationTime * 0.48f + corner * 0.27f + k * 0.035f) % 1f
                val radius = 48f + 205f * easeOutCubic(phase)
                val angleDeg = k * (360f / FIREWORK_STREAKS_PER_CORNER) + celebrationTime * 8f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val streakX = cornerX + radius * cos(angleRad).toFloat()
                val streakY = cornerY + radius * sin(angleRad).toFloat()
                Draw.rectRotated(
                    batch, a.white, streakX, streakY, 8f, 72f * (1f - phase * 0.45f), angleDeg - 90f,
                    FIREWORK_BLUE, (1f - phase) * 0.82f * alpha,
                )
            }
            repeat(FIREWORK_STARS_PER_CORNER) { k ->
                val cyclePos = (celebrationTime * 0.5f + corner * 0.31f + k * 0.05f) % 1f
                val radius = 30f + 240f * easeOutCubic(cyclePos)
                val angleDeg = k * 45f + celebrationTime * 12f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val x = cornerX + radius * cos(angleRad).toFloat()
                val y = cornerY + radius * sin(angleRad).toFloat()
                val size = 64f * (1f - cyclePos * 0.35f)
                val starAlpha = (1f - cyclePos * cyclePos) * alpha
                val tint = tints[(corner + k) % tints.size]
                batch.setColor(tint.r, tint.g, tint.b, starAlpha)
                Draw.imageFitRotated(batch, a.iconStar, x, y, size, size, angleDeg * 2f)
            }
            // Core flash: a bright orb at the burst's origin that blows up and dies fast, so each
            // cycle reads as "something detonated here" instead of stars merely drifting outward.
            val corePos = (celebrationTime * 0.5f + corner * 0.31f) % 1f
            // Deliberately faint: orbGreen is an opaque saturated sprite, so at any real alpha the
            // "flash" reads as a solid green ball parked in the corner rather than a burst of light.
            val coreFade = (1f - corePos * 4f).coerceAtLeast(0f) * 0.3f
            if (coreFade > 0f) {
                val coreSize = 100f + 260f * easeOutCubic(corePos)
                Draw.imageFitTinted(
                    batch, a.uiSkin.orbGreen, cornerX, cornerY, coreSize, coreSize,
                    Theme.lemonYellow, alpha = coreFade * alpha,
                )
            }
        }
        batch.setColor(Color.WHITE)
    }

    /** Confetti — no dedicated confetti asset, so [Assets.white] tinted/rotated into small rects,
     *  the same "raw texture + manual tint/reset" technique [drawFruitRain] already uses for fruit.
     *  Bigger/brighter than the first attempt for the same reason as [drawFireworks]. */
    private fun drawConfetti(batch: SpriteBatch, a: Assets, alpha: Float) {
        val tints = listOf(Theme.cherryRed, Theme.lemonYellow, Theme.peachOrange, Theme.raspberryMaroon, Theme.watermelonGreen, Theme.accentGreen)
        repeat(CONFETTI_COUNT) { index ->
            val speed = 260f + (index % 5) * 30f
            val y = Theme.H + 200f - ((celebrationTime * speed + index * 91f) % (Theme.H + 300f))
            val x = 40f + ((index * 167) % 1000)
            val rotationDeg = index * 47f + celebrationTime * 140f
            val tint = tints[index % tints.size]
            // rectRotated, not imageFitRotated: the latter preserves the 4x4 source's 1:1 aspect and
            // would silently render every "26x38 strip" as a 26x26 square.
            Draw.rectRotated(batch, a.white, x, y, 22f, 40f, rotationDeg, tint, 0.95f * alpha)
        }
    }

    /** Soft gold glow + a slow-rotating star silhouette behind a big prize fruit. Always
     *  [FruitTier.WATERMELON] — a generic celebratory prop, not tied to the actual reward tier (3 of
     *  the 4 dialogs that use `celebration` have no "tier" concept at all, only coins). */
    /** A sparse, slow drift gives defeat panels motion without looking like a reward shower. */
    private fun drawDefeatDrift(batch: SpriteBatch, a: Assets, alpha: Float) {
        repeat(5) { index ->
            val tier = FruitTier.entries[(index + 1) % FruitTier.entries.size]
            val x = 120f + index * 215f
            val y = 230f + ((celebrationTime * (42f + index * 5f) + index * 170f) % 1900f)
            val size = 54f + index % 2 * 14f
            batch.setColor(1f, 1f, 1f, 0.25f * alpha)
            Draw.imageFitRotated(batch, a.region(tier), x, y, size, size, index * 24f - celebrationTime * 12f)
        }
        batch.setColor(Color.WHITE)
    }

    /** A subdued, drooping fruit replaces the watermelon prize on a loss. */
    private fun drawDefeatFruit(batch: SpriteBatch, a: Assets, cx: Float, y: Float) {
        val bob = 7f * sin((celebrationTime * 1.1f).toDouble()).toFloat()
        batch.setColor(0.76f, 0.76f, 0.86f, 0.92f)
        Draw.imageFitRotated(batch, a.region(FruitTier.RASPBERRY), cx, y + bob, 220f, 220f,
            -10f + 3f * sin((celebrationTime * 0.8f).toDouble()).toFloat())
        batch.setColor(Color.WHITE)
    }

    private fun drawDefeatBanner(batch: SpriteBatch, a: Assets, cx: Float, panelTop: Float, scale: Float) {
        val beat = 1f + 0.018f * sin((celebrationTime * 1.4f).toDouble()).toFloat()
        val s = scale * beat
        Ui.fitText(batch, a, "OOPS!", cx, panelTop + 18f * s, 620f * s, 138f * s, Color.WHITE, font = a.victoryXL)
        Ui.fitText(batch, a, "TRY AGAIN", cx, panelTop - 165f * s, 610f * s, 98f * s, Color.WHITE, font = a.victoryTitle)
    }

    private fun layoutDefeatButtons() {
        buttons.forEachIndexed { index, button ->
            button.x = cx - 310f
            button.y = cy - 280f - index * 270f
            button.w = 620f
            button.h = 132f
        }
    }
    private fun drawStarburstAndPrize(batch: SpriteBatch, a: Assets, cx: Float, y: Float) {
        // Low alpha on purpose: orbGreen is a saturated green sprite and SpriteBatch tinting is
        // multiplicative, so it can never actually become gold — at high alpha it reads as a solid
        // green disc covering the panel interior rather than a halo. Kept faint, it passes as glow.
        val pulse = 0.22f + 0.14f * easeOutQuint((celebrationTime % 2f) / 2f)
        Draw.imageFitTinted(batch, a.uiSkin.orbGreen, cx, y, 470f, 470f, Theme.lemonYellow, alpha = pulse)
        batch.setColor(1f, 1f, 1f, 0.45f)
        Draw.imageFitRotated(batch, a.iconStar, cx, y, 450f, 450f, celebrationTime * 18f)
        // Counter-rotating second star doubles the apparent ray count and stops the single star's
        // silhouette from reading as one spinning shape.
        batch.setColor(1f, 1f, 1f, 0.28f)
        Draw.imageFitRotated(batch, a.iconStar, cx, y, 350f, 350f, -celebrationTime * 11f + 22f)
        batch.setColor(Color.WHITE)
        // Slow breathe + a gentle bob so the prize never sits perfectly still.
        val breathe = 1f + 0.05f * (1f - abs(1f - 2f * ((celebrationTime * 0.4f) % 1f)))
        val bob = 10f * sin((celebrationTime * 1.6f).toDouble()).toFloat()
        Draw.imageFitRotated(
            batch, a.region(FruitTier.WATERMELON), cx, y + bob,
            260f * breathe, 260f * breathe, 2.2f * sin((celebrationTime * 1.1f).toDouble()).toFloat(),
        )
    }

    /** Fixed fruit accents, deliberately kept outside the watermelon silhouette. They make every
     * win result feel fruity without relying on random falling sprites to provide the UI content. */
    private fun drawPrizeAccents(batch: SpriteBatch, a: Assets, cx: Float, y: Float) {
        val bob = 7f * sin((celebrationTime * 1.35f).toDouble()).toFloat()
        Draw.imageFit(batch, a.region(FruitTier.LEMON), cx - 235f, y - 125f + bob, 82f, 82f)
        Draw.imageFit(batch, a.region(FruitTier.PEACH), cx + 235f, y - 125f - bob, 82f, 82f)
        Draw.imageFit(batch, a.region(FruitTier.CHERRY), cx - 210f, y + 165f - bob, 86f, 86f)
        Draw.imageFit(batch, a.region(FruitTier.RASPBERRY), cx + 210f, y + 165f + bob, 86f, 86f)
    }
    /** 3 tidy stacks of [Assets.uiSkin.coinBadge], each capped with a decorative multiplier chip
     *  ("10x"/"20x"/"2x", matching the reference design) — not tied to the actual reward math.
     *  A straight vertical stack (no per-coin horizontal jitter) reads as one deliberate pile;
     *  the earlier jittered version scattered the coins enough that each pile looked like loose
     *  overlapping discs rather than a stack. */
    private fun drawCoinClusters(batch: SpriteBatch, a: Assets, cx: Float, y: Float) {
        val chipLabels = listOf("10x", "20x", "2x", "20x", "10x")
        val heights = intArrayOf(3, 5, 4, 5, 3)
        for (i in 0 until 5) {
            val clusterX = cx + (i - 2) * 126f
            val bounce = 4f * sin((celebrationTime * 2f + i * 0.8f).toDouble()).toFloat()
            for (k in 0 until heights[i]) {
                val coinY = y + bounce + k * 14f
                Draw.imageFit(batch, a.uiSkin.coinBadge, clusterX, coinY, 66f, 66f)
                Draw.imageFitRotated(batch, a.iconStar, clusterX, coinY, 29f, 29f, celebrationTime * 24f + k * 13f)
            }
            Ui.fitText(batch, a, chipLabels[i], clusterX, y + bounce + heights[i] * 14f + 34f, 78f, 36f, Color.WHITE, font = a.titleSmall)
        }
    }

    /** The "WOW! / YOU WIN!" banner plaque, pinned to the panel's own top edge (`py + ph`) so it
     *  scales/bounces with the panel's existing pop-in instead of desyncing from it for a frame. */
    private fun drawBanner(batch: SpriteBatch, a: Assets, cx: Float, panelTop: Float, scale: Float) {
        val beat = 1f + 0.035f * (1f - abs(1f - 2f * ((celebrationTime * 0.8f) % 1f)))
        val s = scale * beat
        Ui.fitText(batch, a, "WOW!", cx, panelTop + 18f * s, 690f * s, 150f * s, Color.WHITE, font = a.victoryXL)
        Ui.fitText(batch, a, "YOU WIN!", cx, panelTop - 165f * s, 650f * s, 110f * s, Color.WHITE, font = a.victoryTitle)
    }

    /** Keeps the generated shell's blank CTA artwork while the actual Button hit target and press
     * state stay native. Every celebration dialog has exactly one action. */
    private fun layoutCelebrationButtons() {
        buttons.forEach { button ->
            button.x = cx - CELEBRATION_BUTTON_W / 2f
            button.y = cy - 672f
            button.w = CELEBRATION_BUTTON_W
            button.h = CELEBRATION_BUTTON_H
            button.style = Button.Style.NONE
            button.forcedFont = null
        }
    }

    /** Moving specular star over the live CTA. */
    private fun drawButtonShimmer(batch: SpriteBatch, a: Assets) {
        val button = buttons.firstOrNull() ?: return
        val travel = (celebrationTime * 0.42f) % 1f
        val x = button.x + 74f + travel * (button.w - 148f)
        val alpha = 1f - abs(1f - 2f * travel)
        Draw.imageFitTinted(batch, a.iconStar, x, button.cy + 35f, 40f, 40f, Color.WHITE, alpha = 0.25f + alpha * 0.55f)
    }

    /** 6 twinkling sparkle stars along the panel's silhouette, positioned relative to the (already
     *  pop-in-scaled) panel rect so they track it, drawn topmost/last. Triangle-wave twinkle — no
     *  periodic easing exists in this codebase's `anim` package, so this builds one from [abs]. */
    private fun drawSparkles(batch: SpriteBatch, a: Assets, px: Float, py: Float, pw: Float, ph: Float) {
        val xOffsets = listOf(-60f, pw * 0.5f, pw + 40f, -40f, pw * 0.3f, pw * 0.85f)
        val yOffsets = listOf(ph * 0.8f, ph + 30f, ph * 0.6f, ph * 0.15f, -30f, ph * 0.4f)
        for (index in 0 until 6) {
            val phase = (celebrationTime * 1.3f + index * 0.37f) % 1f
            val twinkle = 1f - abs(1f - 2f * phase)
            val alpha = 0.35f + 0.65f * twinkle
            val size = 40f + 20f * twinkle
            Draw.imageFitTinted(batch, a.iconStar, px + xOffsets[index], py + yOffsets[index], size, size, Color.WHITE, alpha = alpha)
        }
    }

    private companion object {
        const val POP_DURATION = 0.26f
        const val CLOSE_BTN_SIZE = 84f
        const val CLOSE_BTN_MARGIN = 20f
        const val RAIN_FRUIT_COUNT = 18
        const val CELEBRATION_W = 900f
        const val CELEBRATION_H = 1700f
        const val DEFEAT_W = 900f
        const val DEFEAT_H = 1500f
        const val FIREWORK_STARS_PER_CORNER = 8
        const val FIREWORK_STREAKS_PER_CORNER = 12
        const val CONFETTI_COUNT = 24
        // Must stay under jarPanel's own corner inset (34/35px, see Assets.kt) or the outer layer's
        // navy interior peeks through as a thin dark sliver between the two rims instead of reading
        // as one continuous thicker gold band.
        const val CELEBRATION_BUTTON_W = 670f
        const val CELEBRATION_BUTTON_H = 176f
        const val CELEBRATION_PRIZE_Y = 1400f
        const val PRIZE_CLEARANCE_X = 300f
        const val PRIZE_CLEARANCE_Y = 260f

        const val GOD_RAY_COUNT = 16
        // Long enough that a ray's far end always clears the screen corners from screen centre
        // (half-diagonal of 1080x2400 is ~1315), so no ray ever visibly stops mid-air.
        const val GOD_RAY_LENGTH = 2900f
        val FIREWORK_BLUE = Color(0.48f, 0.86f, 1f, 1f)
    }
}
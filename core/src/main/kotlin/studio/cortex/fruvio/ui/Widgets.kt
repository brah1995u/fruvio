package studio.cortex.fruvio.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import studio.cortex.fruvio.Assets
import studio.cortex.fruvio.anim.easeOutBack
import studio.cortex.fruvio.render.Draw

/** A tap target in virtual coordinates with a visual style and press state. */
class Button(
    var x: Float, var y: Float, var w: Float, var h: Float,
    var label: String? = null,
    var region: TextureRegion? = null,
    var style: Style = Style.PRIMARY,
    var role: ActionRole = ActionRole.NONE,
    var leadingIcon: TextureRegion? = null,
    var leadingIconScale: Float = 0.48f,
    var contentGap: Float = 18f,
    var enabled: Boolean = true,
    /** Skips the per-button auto-fit font choice when set — for a row of buttons (e.g.
     *  [studio.cortex.fruvio.screens.MiniGamesScreen]'s three) that must all render their label
     *  at the same size regardless of which one's text happens to be short enough to fit a
     *  bigger font on its own. */
    var forcedFont: BitmapFont? = null,
    val onClick: () -> Unit = {},
) {
    /** Chrome surfaces Fruvio ships — see [Assets.uiSkin]. RED reuses [studio.cortex.fruvio.ui.UiSkin.panel]
     *  (the red nine-patch) for buttons that need to stand out from the default blue. NONE skips
     *  chrome entirely — a bare icon tap target with no panel behind it. */
    enum class Style { PRIMARY, SECONDARY, RED, NONE }

    var pressed = false
    var inputEnabled = true
    /** 0 = just pressed, 1 = fully released/settled. */
    var releaseAnim = 1f

    internal var fittedLabel: String? = null
    internal var fittedFont: BitmapFont? = null
    internal var fittedW = -1f
    internal var fittedH = -1f
    internal var fittedStyle: Style? = null
    internal var renderedLabel: String? = null

    fun contains(px: Float, py: Float) = px >= x && px <= x + w && py >= y && py <= y + h
    val cx get() = x + w / 2f
    val cy get() = y + h / 2f
}

enum class ActionRole { NONE, BACK, HOME, CLOSE, MENU, PAUSE, SHOP }

/** Draws widgets using exactly one exported surface per component. */
object Ui {
    private const val PRESSED_SCALE = 0.94f
    private const val RELEASE_DURATION = 0.22f

    fun tick(b: Button, delta: Float) {
        if (b.pressed) b.releaseAnim = 0f
        else if (b.releaseAnim < 1f) b.releaseAnim = (b.releaseAnim + delta / RELEASE_DURATION).coerceAtMost(1f)
    }

    fun button(batch: SpriteBatch, a: Assets, b: Button) {
        val scale = PRESSED_SCALE + (1f - PRESSED_SCALE) * easeOutBack(b.releaseAnim.coerceIn(0f, 1f))
        val w = b.w * scale; val h = b.h * scale
        val x = b.cx - w / 2f; val y = b.cy - h / 2f
        val labelColor = if (b.enabled) Color.WHITE else Color(1f, 1f, 1f, 0.55f)

        val insets: SafeInsets
        when (b.style) {
            Button.Style.SECONDARY -> {
                // Whole-image scale, not nine-patch: see UiSkin.secondaryButtonRegion doc.
                Draw.imageFit(batch, a.uiSkin.secondaryButtonRegion, b.cx, b.cy, w, h)
                insets = a.uiSkin.primaryButtonInsets
            }
            Button.Style.RED -> {
                Draw.panel(batch, a.uiSkin.panel, x, y, w, h)
                insets = a.uiSkin.panelInsets
            }
            Button.Style.NONE -> {
                insets = a.uiSkin.panelInsets
            }
            else -> {
                Draw.panel(batch, a.uiSkin.primaryButton, x, y, w, h)
                insets = a.uiSkin.primaryButtonInsets
            }
        }
        drawTextButtonContent(batch, a, b, x, y, w, h, labelColor, insets)
    }

    private fun drawTextButtonContent(
        batch: SpriteBatch,
        a: Assets,
        b: Button,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        labelColor: Color,
        insets: SafeInsets,
    ) {
        val label = b.label
        val leading = b.leadingIcon
        if (label != null && leading != null) {
            val safeX = insets.contentX(x, w)
            val safeW = insets.contentWidth(w)
            val safeH = insets.contentHeight(h)
            val iconSize = minOf(h * b.leadingIconScale, safeH * 0.90f)
            val gap = b.contentGap * (w / b.w)

            val baseSafeW = insets.contentWidth(b.w)
            val baseSafeH = insets.contentHeight(b.h)
            val baseIconSize = minOf(b.h * b.leadingIconScale, baseSafeH * 0.90f)
            val baseTextW = (baseSafeW - baseIconSize - b.contentGap).coerceAtLeast(1f)
            val font = fit(a, b, label, baseTextW, baseSafeH, insets)
            val renderedLabel = b.renderedLabel ?: label
            Draw.layout.setText(font, renderedLabel)
            val textWidth = Draw.layout.width.coerceAtMost((safeW - iconSize - gap).coerceAtLeast(1f))

            // Centre the icon+gap+text group as one block instead of pinning the icon to the
            // left edge and centring the label alone in whatever space is left of it — that
            // looked fine for long labels (which fill most of the remaining space anyway) but
            // visibly off-centre for short ones, where the icon+gap pushes the word well right
            // of the button's true centre (e.g. MenuScreen's PLAY/SHOP vs. MINI GAMES/ACHIEVEMENTS).
            val groupW = iconSize + gap + textWidth
            val groupX = safeX + (safeW - groupW) / 2f
            val iconCx = groupX + iconSize / 2f
            val textCx = groupX + iconSize + gap + textWidth / 2f

            Draw.imageFit(batch, leading, iconCx, y + h / 2f, iconSize, iconSize, alpha = if (b.enabled) 1f else 0.55f)
            Draw.textCentered(batch, font, renderedLabel, textCx, y + h / 2f, labelColor)
        } else {
            label?.let {
                val font = fit(a, b, it, insets = insets)
                val rendered = b.renderedLabel ?: it
                if ('\n' in rendered) Draw.textCenteredMultiline(batch, font, rendered, b.cx, b.cy, insets.contentWidth(w), labelColor)
                else Draw.textCentered(batch, font, rendered, b.cx, b.cy, labelColor)
            }
            if (leading == null) b.region?.let { region ->
                val artScale = if (b.style == Button.Style.NONE) 1f else 0.68f
                Draw.imageFit(batch, region, b.cx, b.cy, w * artScale, h * artScale)
            }
        }
    }

    /** Draw the dark Figma panel used by cards, dialogs, loading chrome and game boards. */
    fun panel(batch: SpriteBatch, a: Assets, x: Float, y: Float, w: Float, h: Float) {
        Draw.panel(batch, a.uiSkin.panel, x, y, w, h)
    }

    /** Auto-fit centred text for labels drawn without a button/panel around them, so a
     *  free-floating string (a HUD chip, a caption) can't visually overflow its slot. */
    fun fitText(
        batch: SpriteBatch, a: Assets, label: String, cx: Float, cy: Float, maxW: Float, maxH: Float, color: Color,
        font: BitmapFont? = null,
        /** Skips the per-call auto-scale when set — for a group of peer labels (a wrapped
         *  sentence's two lines, a row of sibling HUD slots) that must all render at the same
         *  size instead of each shrinking independently based on its own text width. Compute with
         *  [textFitScale] over the whole group and pass the minimum. */
        scale: Float? = null,
    ) {
        val font = font ?: chooseFont(a, label, maxW, maxH)
        val scale = scale ?: textFitScale(font, label, maxW, maxH)
        if (scale >= 0.999f) {
            Draw.textCentered(batch, font, label, cx, cy, color)
            return
        }

        val oldScaleX = font.data.scaleX
        val oldScaleY = font.data.scaleY
        font.data.setScale(oldScaleX * scale, oldScaleY * scale)
        Draw.textCentered(batch, font, label, cx, cy, color)
        font.data.setScale(oldScaleX, oldScaleY)
    }

    /** The scale [fitText] would apply to fit [label] into this box on its own. Precompute this
     *  for every label in a peer group and pass the minimum back into [fitText]'s `scale` param
     *  so the whole group renders at one uniform size. */
    fun textFitScale(font: BitmapFont, label: String, maxW: Float, maxH: Float): Float {
        Draw.layout.setText(font, label)
        return minOf(
            1f,
            if (Draw.layout.width > 0f) maxW / Draw.layout.width else 1f,
            if (Draw.layout.height > 0f) maxH / Draw.layout.height else 1f,
        )
    }

    /** Word-wraps [str] across as many lines as [maxW] needs, centred as one block on (cx, cy).
     *  Unlike [fitText], the font never shrinks to squeeze onto one line — a long sentence spills
     *  onto a second line at a fixed, comfortable size instead of blurring down into a font too
     *  small to read cleanly (achievement descriptions, dialog copy). */
    fun wrappedText(batch: SpriteBatch, font: BitmapFont, str: String, cx: Float, cy: Float, maxW: Float, color: Color) {
        Draw.layout.setText(font, str, color, maxW, Align.center, true)
        font.color = color
        font.draw(batch, str, cx - maxW / 2f, cy + Draw.layout.height / 2f, maxW, Align.center, true)
    }

    /** Unframed balance: one coin badge plus a readable amount. */
    fun coinCounter(
        batch: SpriteBatch,
        a: Assets,
        centerX: Float,
        centerY: Float,
        amount: String,
        badgeSize: Float = 94f,
        maxTextW: Float = 180f,
    ) {
        val maxTextH = badgeSize * 0.62f
        val font = when {
            fits(a.num, amount, maxTextW, maxTextH) -> a.num
            fits(a.buttonLight, amount, maxTextW, maxTextH) -> a.buttonLight
            else -> a.captionLight
        }
        val textW = Draw.width(font, amount).coerceAtMost(maxTextW)
        val gap = 14f
        val totalW = badgeSize + gap + textW
        val badgeCx = centerX - totalW / 2f + badgeSize / 2f
        val textCx = badgeCx + badgeSize / 2f + gap + textW / 2f
        Draw.imageFit(batch, a.uiSkin.coinBadge, badgeCx, centerY, badgeSize, badgeSize)
        Draw.imageFit(batch, a.iconStar, badgeCx, centerY, badgeSize * 0.48f, badgeSize * 0.48f)
        Draw.textCentered(batch, font, amount, textCx, centerY, Color.WHITE)
    }

    private fun fit(
        a: Assets,
        b: Button,
        label: String,
        maxWOverride: Float? = null,
        maxHOverride: Float? = null,
        insets: SafeInsets,
    ): BitmapFont {
        val maxW = maxWOverride ?: insets.contentWidth(b.w)
        val maxH = maxHOverride ?: insets.contentHeight(b.h)
        if (b.fittedLabel == label && b.fittedW == maxW && b.fittedH == maxH && b.fittedStyle == b.style) {
            return b.fittedFont ?: a.small
        }
        val chosen = b.forcedFont ?: chooseButtonFont(a, label, maxW, maxH, allowTitle = b.h >= 200f)
        b.fittedLabel = label
        b.fittedW = maxW
        b.fittedH = maxH
        b.fittedStyle = b.style
        b.fittedFont = chosen
        b.renderedLabel = ellipsize(chosen, label, maxW)
        return chosen
    }

    private fun chooseFont(a: Assets, label: String, maxW: Float, maxH: Float): BitmapFont {
        if (fits(a.title, label, maxW, maxH)) return a.title
        if (fits(a.buttonLight, label, maxW, maxH)) return a.buttonLight
        if (fits(a.bodyLight, label, maxW, maxH)) return a.bodyLight
        return a.captionLight
    }

    private fun chooseButtonFont(a: Assets, label: String, maxW: Float, maxH: Float, allowTitle: Boolean): BitmapFont {
        if (allowTitle && fits(a.title, label, maxW, maxH)) return a.title
        if (fits(a.buttonLight, label, maxW, maxH)) return a.buttonLight
        if (fits(a.bodyLight, label, maxW, maxH)) return a.bodyLight
        return a.captionLight
    }

    private fun ellipsize(font: BitmapFont, label: String, maxW: Float): String {
        if (fits(font, label, maxW, Float.MAX_VALUE)) return label
        var end = label.length
        while (end > 1) {
            val candidate = label.substring(0, --end).trimEnd() + "..."
            if (fits(font, candidate, maxW, Float.MAX_VALUE)) return candidate
        }
        return "..."
    }

    private fun fits(font: BitmapFont, label: String, maxW: Float, maxH: Float): Boolean {
        Draw.layout.setText(font, label)
        return Draw.layout.width <= maxW && Draw.layout.height <= maxH
    }
}

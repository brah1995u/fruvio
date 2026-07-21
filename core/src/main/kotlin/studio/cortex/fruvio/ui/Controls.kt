package studio.cortex.fruvio.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import studio.cortex.fruvio.Assets
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.anim.easeOutBack
import studio.cortex.fruvio.render.Draw

/** Animated on/off switch. Not used by any screen in this plan (Settings doesn't exist yet) —
 *  kept so BaseScreen's `toggles`/`sliders` lists type-check now instead of being added later. */
class Toggle(
    var x: Float, var y: Float, var w: Float = 168f, var h: Float = 88f,
    var on: Boolean = true,
    val onChange: (Boolean) -> Unit = {},
) {
    var anim = if (on) 1f else 0f
    fun contains(px: Float, py: Float) = px >= x && px <= x + w && py >= y && py <= y + h
    fun toggle() { on = !on; onChange(on) }
}

/** Horizontal drag slider for a 0..1 value. Same "not used yet" note as [Toggle]. */
class Slider(
    var x: Float, var y: Float, var w: Float, var h: Float = 96f,
    var value: Float = 1f,
    val onChange: (Float) -> Unit = {},
    val onRelease: () -> Unit = {},
) {
    var dragging = false
    var grabAnim = 1f
    fun contains(px: Float, py: Float) = px >= x - 30f && px <= x + w + 30f && py >= y && py <= y + h
    fun setFrom(px: Float) {
        value = ((px - x) / w).coerceIn(0f, 1f)
        onChange(value)
    }
}

/**
 * Rendering and animation for Settings controls. [toggle] uses [Assets.uiSkin]'s `dangerButton`
 * (`ui_btn_rect_red`) as a fixed red pill housing with an `orbGreen` knob that tints grey when
 * off. [slider] has no housing of its own — the row it's drawn on already provides the outer
 * blue-rect frame — and uses the same `orbGreen` knob, always full-colour (sliders have no
 * on/off state).
 */
object Controls {
    private const val TOGGLE_DURATION = 0.20f
    private const val GRAB_DURATION = 0.15f

    fun tick(t: Toggle, delta: Float) {
        val target = if (t.on) 1f else 0f
        val step = delta / TOGGLE_DURATION
        t.anim = if (t.anim < target) (t.anim + step).coerceAtMost(target)
        else (t.anim - step).coerceAtLeast(target)
    }

    fun tick(s: Slider, delta: Float) {
        if (s.dragging) s.grabAnim = 0f
        else if (s.grabAnim < 1f) s.grabAnim = (s.grabAnim + delta / GRAB_DURATION).coerceAtMost(1f)
    }

    fun toggle(batch: SpriteBatch, a: Assets, t: Toggle) {
        // Whole-image scale, not a nine-patch draw — see UiSkin.dangerButton's own doc for why.
        Draw.imageFit(batch, a.uiSkin.dangerButton, t.x + t.w / 2f, t.y + t.h / 2f, t.w, t.h)
        val safe = a.uiSkin.dangerButtonInsets
        val contentX = safe.contentX(t.x, t.w)
        val contentW = safe.contentWidth(t.w)
        val contentY = safe.contentY(t.y, t.h)
        val contentH = safe.contentHeight(t.h)
        val cy = contentY + contentH / 2f
        val r = (contentH * 0.50f).coerceAtMost(25f)
        val minCx = contentX + r
        val maxCx = contentX + contentW - r
        val eased = t.anim * t.anim * (3f - 2f * t.anim)
        val cx = minCx + (maxCx - minCx) * eased
        val labelX = if (t.on) minCx + (maxCx - minCx) * 0.26f else minCx + (maxCx - minCx) * 0.74f

        Draw.textCentered(batch, a.captionLight, if (t.on) "ON" else "OFF", labelX, cy, Color.WHITE)
        if (t.on) {
            Draw.imageFit(batch, a.uiSkin.orbGreen, cx, cy, r * 2f, r * 2f)
        } else {
            Draw.imageFitTinted(batch, a.uiSkin.orbGreen, cx, cy, r * 2f, r * 2f, Color(0.55f, 0.55f, 0.55f, 1f))
        }
    }

    fun slider(batch: SpriteBatch, a: Assets, s: Slider) {
        // No panel/chrome of its own — the row it's drawn on already provides the outer blue-rect
        // frame (see SettingsScreen.drawRow), so the content bounds are just s.x/y/w/h directly,
        // with no inset to carve out of a background this control no longer draws.
        val cy = s.y + s.h / 2f
        val grow = 1.12f - 0.12f * easeOutBack(s.grabAnim.coerceIn(0f, 1f))
        val r = (s.h * 0.60f).coerceAtMost(s.h * 0.34f) * grow
        val minCx = s.x + r
        val maxCx = s.x + s.w - r
        val cx = (minCx + (maxCx - minCx) * s.value).coerceIn(minCx, maxCx)

        batch.setColor(Theme.btnBlue)
        batch.draw(a.white, minCx, cy - 4f, (maxCx - minCx).coerceAtLeast(1f), 8f)
        batch.setColor(Theme.accentGreen)
        batch.draw(a.white, minCx, cy - 4f, (cx - minCx).coerceAtLeast(1f), 8f)
        batch.setColor(Color.WHITE)
        Draw.imageFit(batch, a.uiSkin.orbGreen, cx, cy, r * 2f, r * 2f)
    }
}

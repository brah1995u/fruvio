package studio.cortex.fruvio.ui

import kotlin.math.abs
import kotlin.math.exp

/**
 * Finger-drag vertical scroll state for a viewport rectangle, with inertia. Screens lay
 * out scrolled content each frame at `screenY = top - itemOffset + scroll` and clip with
 * [studio.cortex.fruvio.render.Draw.clipBegin]. A drag only becomes a scroll after
 * [DRAG_THRESHOLD] of travel, so taps on buttons inside the scrolled content still land.
 */
class ScrollArea(var x: Float, var y: Float, var w: Float, var h: Float) {
    var contentH = 0f
    /** 0 = top of content visible; grows as the user scrolls down the list. */
    var scroll = 0f
        private set
    val maxScroll get() = (contentH - h).coerceAtLeast(0f)

    /** True once the current touch has travelled far enough to count as a scroll. */
    var isScrolling = false
        private set

    private var touching = false
    private var lastY = 0f
    private var travelled = 0f
    private var velocity = 0f

    fun contains(px: Float, py: Float) = px >= x && px <= x + w && py >= y && py <= y + h

    fun touchDown(py: Float) {
        touching = true; lastY = py; travelled = 0f; velocity = 0f; isScrolling = false
    }

    /** Returns true while the touch is being treated as a scroll drag. */
    fun touchDragged(py: Float): Boolean {
        if (!touching) return false
        val dy = py - lastY
        lastY = py
        travelled += abs(dy)
        if (travelled > DRAG_THRESHOLD) isScrolling = true
        if (isScrolling) {
            scroll = (scroll + dy).coerceIn(0f, maxScroll)
            velocity = dy * 60f
        }
        return isScrolling
    }

    /** Returns true if THIS touch was a scroll (callers should then swallow the tap). */
    fun touchUp(): Boolean {
        val wasScroll = touching && isScrolling
        touching = false
        isScrolling = false
        return wasScroll
    }

    fun jumpTo(offset: Float) { scroll = offset.coerceIn(0f, maxScroll); velocity = 0f }

    /** Advance inertia; call once per frame. */
    fun tick(delta: Float) {
        if (touching || velocity == 0f) return
        scroll += velocity * delta
        if (scroll <= 0f || scroll >= maxScroll) { scroll = scroll.coerceIn(0f, maxScroll); velocity = 0f; return }
        velocity *= exp(-FRICTION * delta)
        if (abs(velocity) < 12f) velocity = 0f
    }

    private companion object {
        const val DRAG_THRESHOLD = 24f
        const val FRICTION = 5.5f
    }
}

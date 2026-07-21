package studio.cortex.fruvio.ui

import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion

/** Safe content area inside a nine-patch surface, expressed as fractions of its own size. */
data class SafeInsets(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
) {
    init {
        require(left >= 0f && right >= 0f && top >= 0f && bottom >= 0f) { "Safe insets cannot be negative" }
        require(left + right < 1f && top + bottom < 1f) { "Safe insets must leave a positive content area" }
    }

    fun contentWidth(width: Float): Float = width * (1f - left - right)
    fun contentHeight(height: Float): Float = height * (1f - top - bottom)
    fun contentX(x: Float, width: Float): Float = x + width * left
    fun contentY(y: Float, height: Float): Float = y + height * bottom
}

/**
 * Asset-backed UI skin for the buttons and panel actually shipped so far. Insets below are
 * measured from the real PNGs by visually locating where each rounded corner's curve actually
 * resolves into a straight edge (cropped/zoomed corner inspection), not a naive flat-color scan —
 * these buttons have a gradient/glossy fill, so a stable-color heuristic misreads the gradient
 * itself as "still inside the corner." The first measurement pass (a pixel-difference scan) used
 * too-small horizontal insets on `primaryButton`, which let part of the rounded corner fall inside
 * the stretchable middle and visibly warp when the button was resized — re-measured 2026-07-22.
 * Re-measure/extend when more chrome art lands.
 */
class UiSkin(
    val primaryButton: NinePatch,           // ui_btn_rect_blue — wide stacked menu rows
    val secondaryButtonRegion: TextureRegion, // ui_btn_square_blue — square icon buttons (48-96 icon,
    // 120px box). Drawn as a whole scaled image, not nine-patched: its 55px corner curve is
    // comparable to the box size at this scale, so patch-stretching leaves almost no straight
    // middle and the rounded corners visually dominate into a diamond/gem shape instead of a button.
    val panel: NinePatch,                   // ui_panel_square_gold — generic dialog/panel background
    val jarPanel: NinePatch,                // ui_panel_square_gold — dedicated navy gameplay surface
    val coinBadge: TextureRegion,            // ui_badge_circle_gold — coin-pill icon
    // ui_btn_rect_red — Settings toggle track housing. Whole-image scaled, NOT nine-patched: at
    // toggle size (168x88) the source asset's corner radius is comparable to (in fact larger than)
    // the target height, so patch-stretching would collide the top/bottom corner patches into a
    // pinched lens shape — same reasoning as secondaryButtonRegion above, just discovered here via
    // a real render instead of measurement up front.
    val dangerButton: TextureRegion,
    val orbGreen: TextureRegion,             // ui_orb_green — Settings toggle/slider knob
) {
    val primaryButtonInsets = SafeInsets(0.061f, 0.061f, 0.181f, 0.181f)
    val panelInsets = SafeInsets(0.075f, 0.075f, 0.075f, 0.075f)
    // Heuristic safe area for content drawn over the whole-image-scaled dangerButton (not a real
    // nine-patch stretch boundary, just "keep the label/knob off the rounded pill ends").
    val dangerButtonInsets = SafeInsets(0.08f, 0.08f, 0.20f, 0.20f)
}

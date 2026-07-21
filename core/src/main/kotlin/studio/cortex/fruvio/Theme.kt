package studio.cortex.fruvio

import com.badlogic.gdx.graphics.Color

/**
 * Design tokens — the visual swap-point. Palette measured (PIL median-of-opaque-pixels) from
 * the real shipped Figma-derived PNGs — see docs/DESIGN.md §10 and this plan's Task 3 for the
 * exact source file per color; do not re-derive or substitute different hex values. Virtual
 * coordinate space is 1080x2400 (portrait), y-up — unchanged from Flame Jester's proven
 * FitViewport setup, an internal coordinate space unrelated to the Figma export's actual pixel
 * size (1290x2792).
 */
object Theme {
    // Virtual reference resolution (FitViewport)
    const val W = 1080f
    const val H = 2400f
    const val SAFE = 54f // ~5% side safe-area

    // Backgrounds
    val bgWater = rgb(0x48E4FF)      // bg-01.png — underwater/fruit-splash
    val bgTropical = rgb(0x87C7FA)   // bg-02.png — palm/beach
    val bgPanel = rgb(0x144487)      // bg-03.png — solid dotted backdrop, generic menu/UI bg

    // UI chrome
    val btnBlue = rgb(0x4271CB)      // ui_btn_rect_blue / ui_btn_square_blue, averaged
    val btnRed = rgb(0xE55E30)       // ui_btn_rect_red
    val panelDeep = rgb(0x251C60)    // ui_panel_square_gold / ui_badge_circle_gold (measured navy, not gold)
    val accentGreen = rgb(0x84C42C)  // ui_orb_green

    // Fruit reference tints (UI accenting only — the fruit sprites themselves render straight
    // from the atlas, these are for things like a cherry-tinted progress bar elsewhere)
    val cherryRed = rgb(0x78140B)
    val lemonYellow = rgb(0xFFD201)
    val peachOrange = rgb(0xFC8756)
    val raspberryMaroon = rgb(0x99092A)
    val watermelonGreen = rgb(0x4D840B)

    private fun rgb(hex: Int) = Color(
        ((hex shr 16) and 0xFF) / 255f,
        ((hex shr 8) and 0xFF) / 255f,
        (hex and 0xFF) / 255f, 1f,
    )
}

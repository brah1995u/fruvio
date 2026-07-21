package studio.cortex.fruvio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies Theme's rgb() hex->Color bit math against 3 independently-computed channel values —
 * catches a wrong shr/and/divisor bug, not just a transcription typo (each expected value here
 * is computed from the same hex literal via plain arithmetic, independent of Theme's own helper).
 */
class ThemeTest {
    private fun closeEnough(expected: Float, actual: Float) = abs(expected - actual) < 0.002f

    @Test fun bgWaterMatchesMeasuredHex() {
        // bg-01.png (underwater/fruit-splash background), measured hex #48E4FF
        assertTrue(closeEnough(0x48 / 255f, Theme.bgWater.r))
        assertTrue(closeEnough(0xE4 / 255f, Theme.bgWater.g))
        assertTrue(closeEnough(0xFF / 255f, Theme.bgWater.b))
        assertTrue(closeEnough(1f, Theme.bgWater.a))
    }

    @Test fun panelDeepMatchesMeasuredHex() {
        // ui_panel_square_gold.png / ui_badge_circle_gold.png, measured hex #251C60 (deep
        // navy/indigo despite the "gold" filename — filenames are leftover crop-time guesses)
        assertTrue(closeEnough(0x25 / 255f, Theme.panelDeep.r))
        assertTrue(closeEnough(0x1C / 255f, Theme.panelDeep.g))
        assertTrue(closeEnough(0x60 / 255f, Theme.panelDeep.b))
    }

    @Test fun accentGreenMatchesMeasuredHex() {
        // ui_orb_green.png, measured hex #84C42C
        assertTrue(closeEnough(0x84 / 255f, Theme.accentGreen.r))
        assertTrue(closeEnough(0xC4 / 255f, Theme.accentGreen.g))
        assertTrue(closeEnough(0x2C / 255f, Theme.accentGreen.b))
    }
}

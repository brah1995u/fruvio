package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.Sfx
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.Controls
import studio.cortex.fruvio.ui.Slider
import studio.cortex.fruvio.ui.Toggle
import studio.cortex.fruvio.ui.Ui

/** Detailed fruit-themed settings; all values persist and apply immediately. */
class SettingsScreen(game: FruvioGame) : BaseScreen(game) {
    private val backBtn = addBackNavigation { game.toMenu() }
    private val homeBtn = addHomeNavigation()

    private val rowX = 90f
    private val rowW = Theme.W - 180f
    private val rowsTop = 2040f
    private val rowY = (0 until ROWS).map { i -> rowsTop - (i + 1) * ROW_H - i * ROW_GAP }
    private val rowFruit = listOf(FruitTier.CHERRY, FruitTier.LEMON, FruitTier.RASPBERRY, FruitTier.PEACH, FruitTier.WATERMELON)

    private val soundToggle = add(Toggle(
        rowX + rowW - ROW_PADDING - TOGGLE_W, rowY[0] + (ROW_H - TOGGLE_H) / 2f, TOGGLE_W, TOGGLE_H,
        on = game.soundOn, onChange = { enabled ->
            game.setSoundOn(enabled)
            if (enabled) game.audio.play(Sfx.CLICK)
        },
    ))
    private var pendingSfxVolume = game.sfxVolume
    private val sfxVolumeSlider = add(Slider(
        sliderX, rowY[1] + (ROW_H - SLIDER_H) / 2f, SLIDER_W, SLIDER_H,
        value = game.sfxVolume,
        onChange = { v -> pendingSfxVolume = v; game.setSfxVolume(v, flush = false) },
        onRelease = {
            game.setSfxVolume(pendingSfxVolume, flush = true)
            if (game.soundOn) game.audio.play(Sfx.CLICK)
        },
    ))
    private val musicToggle = add(Toggle(
        rowX + rowW - ROW_PADDING - TOGGLE_W, rowY[2] + (ROW_H - TOGGLE_H) / 2f, TOGGLE_W, TOGGLE_H,
        on = game.musicOn, onChange = { game.setMusicOn(it) },
    ))
    private var pendingMusicVolume = game.musicVolume
    private val musicVolumeSlider = add(Slider(
        sliderX, rowY[3] + (ROW_H - SLIDER_H) / 2f, SLIDER_W, SLIDER_H,
        value = game.musicVolume,
        onChange = { v -> pendingMusicVolume = v; game.setMusicVolume(v, flush = false) },
        onRelease = { game.setMusicVolume(pendingMusicVolume, flush = true) },
    ))
    private val vibrationToggle = add(Toggle(
        rowX + rowW - ROW_PADDING - TOGGLE_W, rowY[4] + (ROW_H - TOGGLE_H) / 2f, TOGGLE_W, TOGGLE_H,
        on = game.vibrationOn, onChange = { enabled ->
            game.setVibrationOn(enabled)
            if (enabled) game.buzz(45)
        },
    ))

    private val aboutBtn = add(Button(
        rowX, 550f, rowW, NAV_BTN_H, "ABOUT", style = Button.Style.RED,
        leadingIcon = game.assets.region(FruitTier.PEACH), onClick = { game.toAbout() },
    ))
    private val privacyBtn = add(Button(
        rowX, 390f, rowW, NAV_BTN_H, "PRIVACY POLICY", style = Button.Style.RED,
        leadingIcon = game.assets.region(FruitTier.LEMON), onClick = { game.toPrivacy() },
    ))
    private val termsBtn = add(Button(
        rowX, 230f, rowW, NAV_BTN_H, "TERMS OF USE", style = Button.Style.RED,
        leadingIcon = game.assets.region(FruitTier.RASPBERRY), onClick = { game.toTerms() },
    ))

    override fun draw(delta: Float) {
        // This is intentionally the only screen using the third/reference panel background.
        Draw.cover(batch, a.bgPanel)
        drawDecorations()
        Draw.imageFit(batch, a.iconSettingsFruit, 332f, 2212f, 126f, 126f)
        Ui.fitText(batch, a, "SETTINGS", 620f, 2215f, 470f, 94f, Color.WHITE)
        Draw.textCentered(batch, a.captionLight, "AUDIO  ·  MUSIC  ·  FEEDBACK", Theme.W / 2f, 2112f, Color.WHITE)
        Ui.fitText(batch, a, "FRUIT SOUND MIXER", Theme.W / 2f, 2052f, 500f, 42f, Color.WHITE, font = a.titleSmall)
        drawNavigation(backBtn, homeBtn)

        drawRow(0, "SFX", rowFruit[0])
        drawRow(1, "SFX VOLUME", rowFruit[1])
        drawRow(2, "MUSIC", rowFruit[2])
        drawRow(3, "MUSIC VOLUME", rowFruit[3])
        drawRow(4, "VIBRATION", rowFruit[4])

        Controls.toggle(batch, a, soundToggle)
        Controls.slider(batch, a, sfxVolumeSlider)
        Controls.toggle(batch, a, musicToggle)
        Controls.slider(batch, a, musicVolumeSlider)
        Controls.toggle(batch, a, vibrationToggle)
        drawPercent(game.sfxVolume, rowY[1])
        drawPercent(game.musicVolume, rowY[3])

        drawFruitGarland()
        Draw.imageFit(batch, a.region(FruitTier.LEMON), 330f, 790f, 70f, 70f)
        Ui.fitText(batch, a, "INFO & SUPPORT", Theme.W / 2f, 790f, 400f, 48f, Color.WHITE, font = a.titleSmall)
        Draw.imageFit(batch, a.region(FruitTier.RASPBERRY), 750f, 790f, 70f, 70f)

        Ui.button(batch, a, aboutBtn)
        Ui.button(batch, a, privacyBtn)
        Ui.button(batch, a, termsBtn)
    }

    private fun drawDecorations() {
        val sideFruit = listOf(
            Triple(FruitTier.LEMON, 58f, 1755f),
            Triple(FruitTier.RASPBERRY, Theme.W - 58f, 1548f),
            Triple(FruitTier.PEACH, 58f, 1340f),
            Triple(FruitTier.CHERRY, Theme.W - 58f, 1132f),
        )
        sideFruit.forEach { (tier, x, y) ->
            Draw.imageFit(batch, a.region(tier), x, y, 82f, 82f, alpha = 0.88f)
        }
    }

    private fun drawFruitGarland() {
        FruitTier.entries.forEachIndexed { index, tier ->
            val x = 246f + index * 147f
            val y = 968f + if (index % 2 == 0) 8f else -8f
            Draw.imageFit(batch, a.region(tier), x, y, 72f, 72f, alpha = 0.94f)
        }
    }

    private fun drawRow(i: Int, caption: String, tier: FruitTier) {
        Draw.panel(batch, a.uiSkin.jarPanel, rowX, rowY[i], rowW, ROW_H)
        Draw.imageFit(batch, a.region(tier), rowX + 75f, rowY[i] + ROW_H / 2f, 76f, 76f)
        val maxLabelW = if (i == 1 || i == 3) 190f else 420f
        Ui.fitText(batch, a, caption, rowX + 160f + maxLabelW / 2f, rowY[i] + ROW_H / 2f,
            maxLabelW, 54f, Color.WHITE)
    }

    private fun drawPercent(value: Float, y: Float) {
        Draw.imageFit(batch, a.uiSkin.coinBadge, percentCx, y + ROW_H / 2f, 100f, 100f)
        Ui.fitText(batch, a, "${(value * 100).toInt()}%", percentCx, y + ROW_H / 2f, 78f, 38f, Color.WHITE)
    }

    private val percentCx get() = rowX + rowW - ROW_PADDING - PERCENT_BOX_W / 2f
    private val sliderX get() = rowX + rowW - ROW_PADDING - PERCENT_BOX_W - SLIDER_GAP - SLIDER_W

    private companion object {
        const val ROWS = 5
        const val ROW_H = 176f
        const val ROW_GAP = 28f
        const val ROW_PADDING = 48f
        const val TOGGLE_W = 168f
        const val TOGGLE_H = 88f
        const val SLIDER_W = 320f
        const val SLIDER_H = 96f
        const val SLIDER_GAP = 24f
        const val PERCENT_BOX_W = 140f
        const val NAV_BTN_H = 130f
    }
}
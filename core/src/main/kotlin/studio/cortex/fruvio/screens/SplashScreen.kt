package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.Ui

/** Fruit Cocktail splash with an atlas-backed loading panel and continuation button. */
class SplashScreen(game: FruvioGame) : BaseScreen(game) {
    private var t = 0f
    private var ready = false
    private val continueButton = add(Button(
        (Theme.W - CONTINUE_W) / 2f, CONTINUE_Y, CONTINUE_W, CONTINUE_H, "TAP TO CONTINUE",
        enabled = true, onClick = { game.toMenu() },
    )).apply { inputEnabled = false }

    override fun update(delta: Float) {
        t += delta
        if (!ready && t >= FILL_DURATION) {
            ready = true
            continueButton.inputEnabled = true
        }
    }

    override fun onTouchDown(x: Float, y: Float): Boolean = true

    override fun draw(delta: Float) {
        Draw.cover(batch, a.bgSplashFruvio)
        Draw.textCentered(batch, a.titleXL, "FRUVIO", Theme.W / 2f, 1650f, Color.WHITE)

        if (ready) {
            Ui.button(batch, a, continueButton)
        } else {
            drawLoadingState()
        }

        Draw.textCentered(batch, a.small, "Virtual fun-coins only · No real-world value", Theme.W / 2f, 210f, Color.WHITE)
        Draw.textCentered(batch, a.small, "For entertainment. Play responsibly.", Theme.W / 2f, 170f, Color.WHITE)
    }

    /** The fill covers the whole safe content area so the block itself visibly fills up as
     *  loading progresses, with the label riding on top. [PANEL_CORNER_PX] — not [UiSkin.panelInsets]
     *  — is what actually bounds that area: the nine-patch's corners are a fixed 60px regardless of
     *  the target box size, so a *fraction* of this box's height under-clears them at this aspect
     *  ratio, and the flat-rectangle fill visibly pokes past the chrome's rounded corners. */
    private fun drawLoadingState() {
        val x = (Theme.W - CONTINUE_W) / 2f
        Ui.panel(batch, a, x, CONTINUE_Y, CONTINUE_W, CONTINUE_H)
        val contentX = x + PANEL_CORNER_PX
        val contentW = CONTINUE_W - PANEL_CORNER_PX * 2f
        val contentY = CONTINUE_Y + PANEL_CORNER_PX
        val contentH = CONTINUE_H - PANEL_CORNER_PX * 2f
        batch.setColor(Theme.panelDeep)
        batch.draw(a.white, contentX, contentY, contentW, contentH)
        val progress = (t / FILL_DURATION).coerceIn(0f, 1f)
        batch.setColor(Theme.accentGreen)
        batch.draw(a.white, contentX, contentY, contentW * progress, contentH)
        batch.setColor(Color.WHITE)
        Draw.textCentered(batch, a.bodyLight, "Loading...", Theme.W / 2f, contentY + contentH / 2f, Color.WHITE)
    }

    private companion object {
        const val FILL_DURATION = 1.35f
        const val CONTINUE_W = 640f
        const val CONTINUE_H = 150f
        const val CONTINUE_Y = 638f
        // Matches the 60,60,60,60 pixel insets baked into a.uiSkin.panel's nine-patch (Assets.loadUiSkin).
        const val PANEL_CORNER_PX = 60f
    }
}
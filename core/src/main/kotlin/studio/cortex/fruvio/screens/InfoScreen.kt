package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.ActionRole
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.ScrollArea
import studio.cortex.fruvio.ui.Ui

/** Generic scrollable text screen for About / Privacy Policy / Terms of Use, reached from Settings. */
class InfoScreen(game: FruvioGame, private val heading: String, private val body: String) : BaseScreen(game) {
    private val backBtn = addBackNavigation { game.toSettings() }
    private val homeBtn = addHomeNavigation()

    private val panelX = 70f
    private val panelY = 200f
    private val panelW = Theme.W - 140f
    private val panelH = 1900f
    private val pad = 60f
    private val scroll = add(ScrollArea(panelX, panelY, panelW, panelH))

    override fun draw(delta: Float) {
        Draw.cover(batch, a.bgPanel)
        Draw.textCentered(batch, a.title, heading, Theme.W / 2f, 2210f, Color.WHITE)
        drawNavigation(backBtn, homeBtn)

        Draw.panel(batch, a.uiSkin.jarPanel, panelX, panelY, panelW, panelH)

        val textW = panelW - pad * 2f
        Draw.layout.setText(a.bodyLight, body, Color.WHITE, textW, Align.left, true)
        scroll.contentH = Draw.layout.height + pad * 2f

        if (Draw.clipBegin(batch, game.camera, panelX, panelY, panelW, panelH)) {
            val top = panelY + panelH - pad + scroll.scroll
            a.bodyLight.color = Color.WHITE
            a.bodyLight.draw(batch, body, panelX + pad, top, textW, Align.left, true)
            Draw.clipEnd(batch)
        }
    }
}

object InfoTexts {
    val ABOUT = """
        Fruvio
        by Cortex Studio

        A fruit merge puzzle: drop matching fruit into the jar, merge them into bigger ones, and clear each level's target.

        Twenty levels across four worlds, plus a hall of mini games - Plinko, Bonus Box and Higher or Lower.

        All coins in this game are fun-coins with no real-world value. There is no real-money play, no purchases and no prizes. Play for fun - and play responsibly.
    """.trimIndent()

    val PRIVACY = """
        Last updated: July 2026

        Fruvio is an offline game. We keep data collection to a minimum.

        Your progress and settings are stored locally on your device only - no account is required, and nothing is uploaded to any server.

        The game does not collect personal information, does not track you, and does not access your contacts, photos or location.

        If analytics or ads are added in a future version, this policy will be updated first.
    """.trimIndent()

    val TERMS = """
        Last updated: July 2026

        By using Fruvio, you agree to these terms.

        You may use the game for personal entertainment on your own devices. Do not cheat, exploit bugs, or reverse engineer the app.

        The game is provided "as is", without warranties. We may update features, balance and content at any time.

        All coins and rewards in the game are virtual fun-coins with no monetary value. They cannot be bought, sold or exchanged for real money.
    """.trimIndent()
}

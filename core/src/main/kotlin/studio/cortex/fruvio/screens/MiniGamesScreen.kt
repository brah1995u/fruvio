package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.ActionRole
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.Ui

/**
 * Mini-games hub: 3 stacked buttons routing to [studio.cortex.fruvio.screens.PlinkoScreen],
 * [studio.cortex.fruvio.screens.BonusBoxScreen], and [studio.cortex.fruvio.screens.HigherLowerScreen].
 * Same `Button(x, y, w, h, "LABEL", leadingIcon = ..., onClick = ...)` PRIMARY-style construction
 * [MenuScreen]'s own buttons use, and the same `ROW_H`/`ROW_GAP` rhythm as [SettingsScreen].
 */
class MiniGamesScreen(game: FruvioGame) : BaseScreen(game) {
    private val bw = 700f
    private val bx = Theme.W / 2f - bw / 2f

    private val backBtn = addBackNavigation { game.toMenu() }
    private val homeBtn = addHomeNavigation()

    // forcedFont: without it, the auto-fit picks the biggest font that fits *each label on its
    // own* — "FRUIT BOX" comfortably fits the big title font at this button height while the
    // longer "FRUIT PLINKO"/"FRUIT GUESS" don't, so the three ended up at visibly different
    // sizes. Pinning all three to the size the longest label needs keeps them uniform.
    private val plinko = add(Button(
        bx, 1500f, bw, ROW_H, "FRUIT PLINKO", leadingIcon = game.assets.region(FruitTier.WATERMELON),
        forcedFont = game.assets.buttonLight, onClick = { game.toPlinko() },
    ))
    private val bonusBox = add(Button(
        bx, 1260f, bw, ROW_H, "FRUIT BOX", leadingIcon = game.assets.region(FruitTier.LEMON),
        forcedFont = game.assets.buttonLight, onClick = { game.toBonusBox() },
    ))
    private val higherLower = add(Button(
        bx, 1020f, bw, ROW_H, "FRUIT GUESS", leadingIcon = game.assets.region(FruitTier.PEACH),
        forcedFont = game.assets.buttonLight, onClick = { game.toHigherLower() },
    ))

    override fun draw(delta: Float) {
        Draw.cover(batch, a.fruitBackground(3))
        Draw.textCentered(batch, a.title, "MINI GAMES", Theme.W / 2f, 2220f, Color.WHITE)
        drawNavigation(backBtn, homeBtn)

        Ui.button(batch, a, plinko)
        Ui.button(batch, a, bonusBox)
        Ui.button(batch, a, higherLower)
    }

    private companion object {
        const val ROW_H = 200f
    }
}

package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.Ui

/**
 * Main menu: coin balance top-left, Settings icon top-right, title, then Play / Mini Games /
 * Shop / Achievements stacked (same reference layout Flame Jester's own MenuScreen uses — see
 * docs/DESIGN.md §3/§8). Play routes to [studio.cortex.fruvio.screens.LevelSelectScreen],
 * Settings to [studio.cortex.fruvio.screens.SettingsScreen], Mini Games to
 * [studio.cortex.fruvio.screens.MiniGamesScreen], Shop to [studio.cortex.fruvio.screens.ShopScreen],
 * and Achievements to [studio.cortex.fruvio.screens.AchievementsScreen].
 */
class MenuScreen(game: FruvioGame) : BaseScreen(game) {
    private val play = add(Button(
        Theme.W / 2f - 420f, 1045f, 840f, 215f, "PLAY", leadingIcon = game.assets.iconPlay,
        leadingIconScale = 0.54f, style = Button.Style.RED,
        onClick = { game.toLevelSelect() },
    ))
    private val miniGames = add(Button(
        Theme.W / 2f - 360f, 820f, 720f, 170f, "MINI GAMES", leadingIcon = game.assets.iconMiniGames,
        leadingIconScale = 0.48f, style = Button.Style.RED,
        onClick = { game.toMiniGames() },
    ))
    private val shop = add(Button(
        Theme.W / 2f - 320f, 620f, 640f, 145f, "SHOP", leadingIcon = game.assets.iconNavCart,
        leadingIconScale = 0.44f, style = Button.Style.RED, onClick = { game.toShop() },
    ))
    private val achievements = add(Button(
        Theme.W / 2f - 320f, 440f, 640f, 145f, "ACHIEVEMENTS", leadingIcon = game.assets.iconNavStar,
        leadingIconScale = 0.44f, style = Button.Style.RED,
        onClick = { game.toAchievements() },
    ))
    // RED chrome (matching the four menu rows) rather than a bare chrome-less icon, and
    // iconSettingsFruit rather than the generic hamburger: this button opens Settings, so the
    // fruit-gear icon already in the atlas вЂ” which nothing else used вЂ” is both on-brand and
    // semantically clearer than three neutral bars floating on the background.
    // 150px, not the 120px every other screen's nav button uses: the RED chrome is a nine-patch with
    // fixed 60px corners, so at exactly 120px the top and bottom corner patches meet with zero
    // straight middle and the frame renders pinched. 150 leaves 30px of real stretchable middle.
    private val settings = add(Button(
        Theme.W - 48f - 150f, 2214f, 150f, 150f, style = Button.Style.RED,
        region = game.assets.iconSettingsFruit, onClick = { game.toSettings() },
    ))

    override fun draw(delta: Float) {
        Draw.cover(batch, a.bgMenuCocktail)

        Ui.coinCounter(batch, a, 180f, 2304f, "%,d".format(game.progress.coins), badgeSize = 104f, maxTextW = 210f)

        // A little extra fruit clustered in the gap under the settings badge — not touching the
        // button above (bottom edge 2214) or the title below, so it reads as a corner accent
        // instead of clutter on top of tappable/readable elements.
        Draw.imageFit(batch, a.region(FruitTier.LEMON), 950f, 2130f, 120f, 120f, alpha = 0.82f)
        Draw.imageFit(batch, a.region(FruitTier.RASPBERRY), 985f, 2020f, 90f, 90f, alpha = 0.78f)

        Draw.textCentered(batch, a.title, "FRUVIO", Theme.W / 2f, 1980f, Color.WHITE)
        Draw.textCentered(batch, a.bodyLight, "Fruit Merge Puzzle", Theme.W / 2f, 1890f, Color.WHITE)
        Draw.imageFit(batch, a.region(FruitTier.WATERMELON), Theme.W / 2f, 1520f, 420f, 420f)

        Ui.button(batch, a, play)
        Ui.button(batch, a, miniGames)
        Ui.button(batch, a, shop)
        Ui.button(batch, a, achievements)
        Ui.button(batch, a, settings)

        Draw.textCentered(batch, a.small, "Offline fun-coins only · No real-money play", Theme.W / 2f, 300f, Color.WHITE)
    }
}

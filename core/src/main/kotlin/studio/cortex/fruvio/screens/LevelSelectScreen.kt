package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.merge.LevelDef
import studio.cortex.fruvio.engine.merge.Levels
import studio.cortex.fruvio.engine.merge.WinCondition
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.ActionRole
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.ScrollArea
import studio.cortex.fruvio.ui.Ui

/** Four-world, twenty-level campaign picker with sequential unlock and persistent stars. */
class LevelSelectScreen(game: FruvioGame) : BaseScreen(game) {
    private val backBtn = addBackNavigation { game.toMenu() }
    private val homeBtn = addHomeNavigation()

    private val viewX = 70f
    private val viewY = 90f
    private val viewW = Theme.W - 140f
    private val viewH = 2020f
    private val viewTop = viewY + viewH
    private val cardX = viewX + 20f
    private val cardW = viewW - 40f

    private val headerOffsets = FloatArray(Levels.worlds.size)
    private val cardOffsets = FloatArray(Levels.all.size)
    private val contentHeight: Float

    init {
        var cursor = 28f
        Levels.worlds.forEachIndexed { worldIndex, world ->
            headerOffsets[worldIndex] = cursor
            cursor += WORLD_HEADER_H + HEADER_GAP
            Levels.all.filter { it.worldIndex == world.index }.forEach { level ->
                cardOffsets[level.index - 1] = cursor
                cursor += CARD_H + CARD_GAP
            }
            cursor += WORLD_GAP
        }
        contentHeight = (cursor - WORLD_GAP).coerceAtLeast(viewH)
    }

    private val scrollArea = add(ScrollArea(viewX, viewY, viewW, viewH)).apply { contentH = contentHeight }

    private val cards = Levels.all.map { level ->
        add(Button(cardX, 0f, cardW, CARD_H, onClick = { game.toMerge(level) }))
    }

    // The text column, and one shared font scale per line computed across ALL 20 levels. Without
    // the shared scale each card auto-fits independently, so "1. FIRST SPLASH" renders visibly
    // larger than "5. BAY WATERMELON" and "SCORE 120" larger than "SCORE 220 IN 18 DROPS" —
    // a scrolling list of mismatched type sizes. Taking the worst case makes the whole list uniform.
    private val textW = cardW - TEXT_LEFT_INSET - TEXT_RIGHT_INSET
    private val textCx = cardX + TEXT_LEFT_INSET + textW / 2f
    private val titleScale = Levels.all.minOf {
        Ui.textFitScale(game.assets.buttonLight, cardTitle(it), textW, TITLE_MAX_H)
    }
    private val objectiveScale = Levels.all.minOf {
        Ui.textFitScale(game.assets.bodyLight, objective(it), textW, OBJECTIVE_MAX_H)
    }
    private val metaScale = Levels.all.minOf {
        Ui.textFitScale(game.assets.captionLight, meta(it), textW, META_MAX_H)
    }

    override fun update(delta: Float) {
        Levels.all.forEachIndexed { index, level ->
            val button = cards[index]
            button.y = viewTop - cardOffsets[index] - CARD_H + scrollArea.scroll
            button.enabled = index == 0 || Levels.all.take(index).all { game.progress.stars(it.index) > 0 }
            button.inputEnabled = button.y < viewTop && button.y + CARD_H > viewY
        }
    }

    override fun draw(delta: Float) {
        Draw.cover(batch, a.fruitBackground(2))
        Draw.textCentered(batch, a.title, "SELECT LEVEL", Theme.W / 2f, 2220f, Color.WHITE)
        drawNavigation(backBtn, homeBtn)
        if (Draw.clipBegin(batch, game.camera, viewX, viewY, viewW, viewH)) {
            Levels.worlds.forEachIndexed { index, world ->
                val centerY = viewTop - headerOffsets[index] - WORLD_HEADER_H / 2f + scrollArea.scroll
                if (centerY in (viewY - WORLD_HEADER_H)..(viewTop + WORLD_HEADER_H)) {
                    val fruit = worldFruit(world.index)
                    Draw.imageFit(batch, a.region(fruit), 228f, centerY, 70f, 70f)
                    Ui.fitText(batch, a, "WORLD ${world.index} · ${world.displayName}", 610f, centerY, 650f, 60f, Color.WHITE)
                }
            }
            Levels.all.forEachIndexed { index, level -> drawCard(level, cards[index]) }
            Draw.clipEnd(batch)
        }
    }

    private fun drawCard(level: LevelDef, button: Button) {
        if (button.y + CARD_H < viewY || button.y > viewTop) return
        Draw.panel(batch, a.uiSkin.primaryButton, button.x, button.y, button.w, button.h,
            if (button.enabled) Color.WHITE else Color(0.48f, 0.48f, 0.56f, 1f))
        val iconCx = button.x + 92f
        Draw.imageFit(batch, a.region(cardFruit(level)), iconCx, button.cy, 112f, 112f,
            alpha = if (button.enabled) 1f else 0.48f)
        // All three lines sit inside the nine-patch's straight-edge interior (CARD_CORNER_PX in
        // from top and bottom). The previous layout anchored them at CARD_H-58 and +50 — literally
        // above and below that band — so the tallest glyphs rode up into the chrome's rounded top
        // corners, which is what read as "text not fitting the frame".
        Ui.fitText(batch, a, cardTitle(level), textCx, button.y + TITLE_CY, textW, TITLE_MAX_H,
            Color.WHITE, font = a.buttonLight, scale = titleScale)
        Ui.fitText(batch, a, objective(level), textCx, button.y + OBJECTIVE_CY, textW, OBJECTIVE_MAX_H,
            Color.WHITE, font = a.bodyLight, scale = objectiveScale)
        Ui.fitText(batch, a, meta(level), textCx, button.y + META_CY, textW, META_MAX_H,
            Color.WHITE, font = a.captionLight, scale = metaScale)
        if (button.enabled) {
            val stars = game.progress.stars(level.index)
            for (star in 0 until 3) {
                Draw.imageFit(batch, a.iconStar, button.x + button.w - 166f + star * 54f,
                    button.y + CARD_H / 2f, 42f, 42f, alpha = if (star < stars) 1f else 0.24f)
            }
        } else {
            Ui.fitText(batch, a, "LOCKED", button.x + button.w - 112f, button.y + CARD_H / 2f,
                160f, 42f, Color.WHITE)
        }
    }

    private fun cardFruit(level: LevelDef): FruitTier = when (val condition = level.winCondition) {
        is WinCondition.ReachTier -> condition.target
        else -> FruitTier.entries[(level.index - 1).mod(FruitTier.entries.size)]
    }

    private fun worldFruit(worldIndex: Int): FruitTier =
        FruitTier.entries[((worldIndex - 1) * 2).mod(FruitTier.entries.size)]

    private fun cardTitle(level: LevelDef): String = "${level.index}. ${level.displayName.uppercase()}"

    private fun meta(level: LevelDef): String =
        "${level.jarWidthUnits}x${level.jarHeightUnits} JAR · PAR ${level.parValue}"

    private fun objective(level: LevelDef): String = when (val condition = level.winCondition) {
        is WinCondition.ScoreThreshold -> "SCORE ${condition.target}"
        is WinCondition.ReachTier -> "REACH ${condition.target.name}"
        is WinCondition.DropLimit -> "SCORE ${condition.scoreThreshold} IN ${condition.maxDrops} DROPS"
        is WinCondition.ComboChallenge -> "COMBO ${condition.minStreak} + SCORE ${condition.scoreTarget}"
    }

    internal fun debugShowWorld(worldIndex: Int) {
        val offset = headerOffsets[(worldIndex - 1).coerceIn(0, headerOffsets.lastIndex)]
        scrollArea.jumpTo(offset)
    }

    private companion object {
        const val WORLD_HEADER_H = 82f
        const val HEADER_GAP = 20f
        const val CARD_H = 238f
        const val CARD_GAP = 24f
        const val WORLD_GAP = 46f

        /** The card chrome is `uiSkin.primaryButton`, a nine-patch with fixed 60px pixel corners
         *  (see Assets.loadUiSkin) — NOT a fraction of the card's height. Content must stay between
         *  y+60 and y+CARD_H-60, i.e. a 118px-tall straight-edge band, or it overlaps the rounded
         *  corner art. Same class of bug as the CLAIM-button pinch and the splash loading bar. */
        const val CARD_CORNER_PX = 60f

        const val TEXT_LEFT_INSET = 154f
        // Clears the 3-star column, which starts at cardW - 166 and is 42px wide per star.
        const val TEXT_RIGHT_INSET = 210f

        // Three lines stacked inside that band, largest at top, derived from the corner inset so
        // they cannot drift back outside it if CARD_H is ever retuned.
        const val TITLE_CY = CARD_H - CARD_CORNER_PX - 26f
        const val TITLE_MAX_H = 40f
        const val OBJECTIVE_CY = CARD_H - CARD_CORNER_PX - 66f
        const val OBJECTIVE_MAX_H = 32f
        const val META_CY = CARD_CORNER_PX + 16f
        const val META_MAX_H = 26f
    }
}
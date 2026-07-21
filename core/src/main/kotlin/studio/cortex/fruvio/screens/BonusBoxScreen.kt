package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.MiniGame
import studio.cortex.fruvio.Sfx
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.anim.easeOutBack
import studio.cortex.fruvio.engine.bonusbox.BonusBoxGame
import studio.cortex.fruvio.engine.bonusbox.BonusBoxRewards
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.merge.SeededRng
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.ActionRole
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.Dialog
import studio.cortex.fruvio.ui.Ui
import kotlin.random.Random

/**
 * Scratch-card mini-game: idle screen shows one "PLAY" button; tapping it spends
 * [BonusBoxRewards.PLAY_COST] coins up front and deals a [BonusBoxGame] as a face-down 3x3 grid.
 * The player picks [BonusBoxRewards.PICK_COUNT] cards (each a real [BonusBoxGame.reveal] call);
 * once the round completes the remaining cards auto-reveal in a cosmetic cascade (they never touch
 * [BonusBoxGame.totalPayout]) before the payout dialog opens.
 */
class BonusBoxScreen(game: FruvioGame) : BaseScreen(game) {
    private val backBtn = addBackNavigation { game.toMiniGames() }
    private val homeBtn = addHomeNavigation()

    // ---- 3x3 grid geometry: centred in the play area below the title, above the bottom margin ----
    private val gridSize = 3 * CELL_SIZE + 2 * CELL_GAP
    private val gridOriginX = (Theme.W - gridSize) / 2f
    private val gridOriginY = PLAY_AREA_BOTTOM + (PLAY_AREA_TOP - PLAY_AREA_BOTTOM - gridSize) / 2f

    private val cardButtons: List<Button> = (0 until BonusBoxRewards.CARD_COUNT).map { i ->
        val col = i % 3
        val row = i / 3
        val x = gridOriginX + col * (CELL_SIZE + CELL_GAP)
        val y = gridOriginY + (2 - row) * (CELL_SIZE + CELL_GAP)
        add(Button(x, y, CELL_SIZE, CELL_SIZE, onClick = { onCardTapped(i) })).also { it.inputEnabled = false }
    }

    private val playBtn = add(Button(
        Theme.W / 2f - 320f, gridOriginY + gridSize / 2f - 80f, 640f, 160f,
        "PLAY - ${BonusBoxRewards.PLAY_COST} coins",
        onClick = { onPlayTapped() },
    ))

    // ---- round state ----
    private var bonusBoxGame: BonusBoxGame? = null
    private val revealedCards = HashMap<Int, FruitTier>() // cardIndex -> tier, for both real and cascade reveals
    private val cardPunchElapsed = HashMap<Int, Float>() // cardIndex -> seconds since its reveal flip started

    /** True once the round's 3rd real pick has landed — gates [update] (mirroring
     *  `MergeGameScreen`'s `finished`) while the cosmetic cascade + payout dialog play out. */
    private var roundDone = false

    private var cascadeActive = false
    private var cascadeElapsed = 0f
    private var cascadeStepIndex = 0
    private var cascadeOrder: List<Int> = emptyList()

    // ---- per-frame update ----
    override fun update(delta: Float) {
        if (roundDone) return
        // No continuous simulation runs here — reveals are tap-driven via onCardTapped, not
        // per-frame. This override exists to hold the roundDone guard, matching the house
        // update()/draw() split convention (see draw()'s doc on why the juice timers live there).
    }

    private fun onPlayTapped() {
        if (game.progress.coins < BonusBoxRewards.PLAY_COST) return
        startNewRound()
    }

    private fun startNewRound() {
        game.progress.coins -= BonusBoxRewards.PLAY_COST
        bonusBoxGame = BonusBoxGame(SeededRng(Random.nextLong()))
        revealedCards.clear()
        cardPunchElapsed.clear()
        roundDone = false
        cascadeActive = false
        cascadeElapsed = 0f
        cascadeStepIndex = 0
        cascadeOrder = emptyList()
        cardButtons.forEach { it.inputEnabled = true }
        playBtn.inputEnabled = false
    }

    private fun onCardTapped(i: Int) {
        val g = bonusBoxGame ?: return
        if (g.done || i in revealedCards) return
        val events = g.reveal(i)
        revealedCards[i] = g.cards[i]
        cardPunchElapsed[i] = 0f
        cardButtons[i].inputEnabled = false
        for (e in events) when (e) {
            is BonusBoxGame.Revealed -> Unit // visual state already applied above
            is BonusBoxGame.RoundComplete -> beginCascade()
        }
    }

    /** Starts the cosmetic auto-reveal of the [BonusBoxRewards.CARD_COUNT] - [BonusBoxRewards.PICK_COUNT]
     *  cards the player never picked. Purely visual — these never call [BonusBoxGame.reveal], so
     *  they can't affect [BonusBoxGame.totalPayout]. */
    private fun beginCascade() {
        roundDone = true
        cardButtons.forEach { it.inputEnabled = false }
        val remaining = (0 until BonusBoxRewards.CARD_COUNT).filter { it !in revealedCards }
        cascadeOrder = remaining
        cascadeElapsed = 0f
        cascadeStepIndex = 0
        if (remaining.isEmpty()) finishRound() else cascadeActive = true
    }

    private fun finishRound() {
        val g = bonusBoxGame ?: return
        game.progress.markMiniGamePlayed(MiniGame.BONUS_BOX)
        val netWin = g.totalPayout >= BonusBoxRewards.PLAY_COST
        if (!netWin) game.progress.earnCoins(g.totalPayout)
        game.audio.play(if (netWin) Sfx.WIN else Sfx.LOSE)
        game.buzz(if (netWin) 60 else 70)
        openRoundCompleteDialog(g.totalPayout)
    }

    private fun openRoundCompleteDialog(totalPayout: Long) {
        val net = totalPayout - BonusBoxRewards.PLAY_COST
        val netWin = net >= 0
        val d = Dialog(760f, 820f, if (netWin) "BONUS WIN!" else "ROUND COMPLETE",
            onDismiss = { game.toMiniGames() })
        d.fruitRain = netWin
        d.celebration = netWin
        d.defeat = !netWin
        // Content sits lower on the celebration panel (the prize fruit and banner own the top of it)
        // than on the compact loss panel, so every row's drop below the panel top is branch-specific.
        val iconDrop = if (netWin) 940f else 420f
        val coinsDrop = if (netWin) 1_050f else 710f
        val netDrop = if (netWin) 1_120f else 780f
        d.drawContent = { b, assets, px, py, pw, ph ->
            val fruitY = py + ph - iconDrop
            if (!netWin) {
                Draw.imageFit(b, assets.region(FruitTier.LEMON), px + pw / 2f - 205f, fruitY - 25f, 78f, 78f)
                Draw.imageFit(b, assets.region(FruitTier.RASPBERRY), px + pw / 2f, fruitY + 28f, 150f, 150f)
                Draw.imageFit(b, assets.region(FruitTier.PEACH), px + pw / 2f + 205f, fruitY - 25f, 78f, 78f)
            }
            Ui.fitText(b, assets, "+$totalPayout COINS", px + pw / 2f, py + ph - coinsDrop, pw - 160f, 52f, Color.WHITE)
            Ui.fitText(b, assets, if (net >= 0) "NET +$net" else "NET $net", px + pw / 2f, py + ph - netDrop,
                pw - 180f, 42f, if (net >= 0) Theme.accentGreen else Theme.peachOrange)
        }
        val canPlayAgain = game.progress.coins >= BonusBoxRewards.PLAY_COST
        // The loss branch keeps the original compact panel (760x820), so it must keep the original
        // button rows too — the celebration rows are positioned for the taller 860x1080 panel and
        // would sit below a non-celebration panel's bottom edge.
        // Loss rows sit at cy-160/cy-320, not the shallower cy-40/cy-200 the other mini-games use:
        // this dialog stacks three content rows (icons, payout, NET) reaching down to ~cy+30, so a
        // shallower first button would cover the payout line.
        if (!netWin) d.buttons += Button(
            d.cx - 280f, d.cy - 160f, 560f, 130f, "PLAY AGAIN",
            enabled = canPlayAgain, onClick = { closeDialog(); startNewRound() },
        )
        d.buttons += if (netWin) {
            Button(d.cx - 350f, d.cy - 480f, 700f, 176f, "COLLECT ALL\nREWARDS!", onClick = {
                game.progress.earnCoins(totalPayout)
                game.toMiniGames()
            })
        } else {
            Button(d.cx - 280f, d.cy - 320f, 560f, 130f, "BACK TO MINI GAMES", onClick = { game.toMiniGames() })
        }
        openDialog(d)
    }

    internal fun debugShowWinDialog() = openRoundCompleteDialog(195L)
    internal fun debugShowLossDialog() = openRoundCompleteDialog(5L)

    // ---- render ----
    override fun draw(delta: Float) {
        // tickEffects() MUST run unconditionally here, not in update(delta): update() bails out
        // early once roundDone is set (see beginCascade(), invoked from the same onCardTapped()
        // call that starts the cascade) — ticking there would freeze the flip-punch and cascade
        // timers on the very frame the round completes. Mirrors MergeGameScreen's draw()/tickEffects().
        tickEffects(delta)

        Draw.cover(batch, a.fruitBackground(6))
        Ui.coinCounter(batch, a, Theme.W - 320f, 2304f, "%,d".format(game.progress.coins), badgeSize = 90f, maxTextW = 180f)
        Draw.textCentered(batch, a.title, "BONUS BOX", Theme.W / 2f, 2220f, Color.WHITE)
        drawNavigation(backBtn, homeBtn)

        if (bonusBoxGame == null) {
            playBtn.enabled = game.progress.coins >= BonusBoxRewards.PLAY_COST
            Ui.button(batch, a, playBtn)
        } else {
            drawGrid()
        }
    }

    private fun tickEffects(delta: Float) {
        for (i in cardPunchElapsed.keys) cardPunchElapsed[i] = cardPunchElapsed.getValue(i) + delta

        if (cascadeActive) {
            cascadeElapsed += delta
            while (cascadeActive && cascadeElapsed >= (cascadeStepIndex + 1) * CASCADE_STEP_DELAY) {
                val idx = cascadeOrder[cascadeStepIndex]
                revealedCards[idx] = bonusBoxGame!!.cards[idx]
                cardPunchElapsed[idx] = 0f
                cascadeStepIndex++
                if (cascadeStepIndex >= cascadeOrder.size) {
                    cascadeActive = false
                    finishRound()
                }
            }
        }
    }

    private fun cardScale(i: Int): Float {
        val elapsed = cardPunchElapsed[i] ?: return 1f
        return easeOutBack((elapsed / FLIP_PUNCH_DURATION).coerceIn(0f, 1f))
    }

    private fun drawGrid() {
        for (i in 0 until BonusBoxRewards.CARD_COUNT) {
            val btn = cardButtons[i]
            val size = CELL_SIZE * cardScale(i)
            Draw.panel(batch, a.uiSkin.panel, btn.cx - size / 2f, btn.cy - size / 2f, size, size)

            val tier = revealedCards[i]
            if (tier == null) {
                Draw.textCentered(batch, a.title, "?", btn.cx, btn.cy, Color.WHITE)
            } else {
                Draw.imageFit(batch, a.region(tier), btn.cx, btn.cy, CELL_SIZE * 0.6f, CELL_SIZE * 0.6f)
                val reward = BonusBoxRewards.COIN_REWARD.getValue(tier)
                Draw.textCentered(batch, a.bodyLight, "+$reward", btn.cx, btn.cy - CELL_SIZE * 0.35f, Color.WHITE)
            }
        }
    }

    private companion object {
        const val CELL_SIZE = 300f
        const val CELL_GAP = 30f
        const val PLAY_AREA_TOP = 2080f
        const val PLAY_AREA_BOTTOM = 150f
        const val FLIP_PUNCH_DURATION = 0.26f
        const val CASCADE_STEP_DELAY = 0.05f
    }
}

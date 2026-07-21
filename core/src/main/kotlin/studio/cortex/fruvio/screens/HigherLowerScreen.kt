package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.MiniGame
import studio.cortex.fruvio.Sfx
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.anim.easeOutBack
import studio.cortex.fruvio.anim.easeOutQuint
import studio.cortex.fruvio.engine.higherlower.HigherLowerGame
import studio.cortex.fruvio.engine.higherlower.HigherLowerRules
import studio.cortex.fruvio.engine.merge.SeededRng
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.ActionRole
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.Dialog
import studio.cortex.fruvio.ui.Ui
import kotlin.random.Random

/**
 * Higher-Lower mini-game: stake [HigherLowerRules.STAKE] coins, guess whether the next fruit tier
 * is higher or lower than the shown one, and either keep pushing the streak or cash out for
 * [HigherLowerRules.payout]. [HigherLowerGame] (`engine/higherlower`, pure logic) owns
 * streak/alive bookkeeping; this screen is purely reactive to the [HigherLowerGame.Event]s each
 * `guess`/`cashOut` call returns — there is no per-frame simulation to step (unlike
 * [MergeGameScreen]'s physics), so [update] only refreshes button availability each frame.
 */
class HigherLowerScreen(game: FruvioGame) : BaseScreen(game) {
    private var hlGame: HigherLowerGame? = null
    private var finished = false

    // ---- juice: card reveal punch (keyed by an ever-incrementing counter, NOT by FruitTier
    // value — see the doc on revealCounter below), push-tie text flash, win/lose moment-flash ----

    /** Bumped on every card reveal — the very first draw of a round, every push redraw, and
     *  every real guess resolution (Correct/Busted alike). [FruitTier] only has 5 values, so two
     *  consecutive reveals can easily share the same tier even though they're different draws;
     *  keying the punch map by this ever-increasing counter (instead of by tier identity) means
     *  every reveal always gets a brand-new map entry and therefore always re-triggers the
     *  animation — a same-valued reveal keyed by tier value would silently fail to re-trigger
     *  since the map key wouldn't register as "new." Same class of footgun as MergeGameScreen's
     *  documented Box2D body-pooling aliasing bug (a stale/reused key silently suppresses an
     *  update), different mechanism (value-collision here vs. reference-pooling there). */
    private var revealCounter = 0
    private data class CardPunch(var elapsed: Float)
    private val cardPunches = HashMap<Int, CardPunch>()
    private fun triggerCardPunch() { revealCounter++; cardPunches[revealCounter] = CardPunch(0f) }

    private data class PushFlash(var elapsed: Float)
    private val pushFlashes = ArrayList<PushFlash>()

    private var momentFlashElapsed = Float.MAX_VALUE
    private var momentFlashColor: Color = Color.WHITE
    private fun triggerMomentFlash(color: Color) { momentFlashElapsed = 0f; momentFlashColor = color }

    private val backBtn = addBackNavigation { game.toMiniGames() }
    private val homeBtn = addHomeNavigation()

    private val playBtn = add(Button(
        (Theme.W - PLAY_BTN_W) / 2f, PLAY_BTN_Y, PLAY_BTN_W, PLAY_BTN_H,
        "PLAY — ${HigherLowerRules.STAKE} COINS", onClick = { startRound() },
    ))

    private val cashOutBtn = add(Button(
        (Theme.W - CASHOUT_BTN_W) / 2f, CASHOUT_BTN_Y, CASHOUT_BTN_W, CASHOUT_BTN_H,
        "CASH OUT", onClick = { onCashOut() },
    )).apply { inputEnabled = false }

    private val higherBtn = add(Button(
        GUESS_BTN_START_X, GUESS_BTN_Y, GUESS_BTN_W, GUESS_BTN_H,
        "HIGHER", onClick = { onGuess(HigherLowerGame.Guess.HIGHER) },
    )).apply { inputEnabled = false }

    private val lowerBtn = add(Button(
        GUESS_BTN_START_X + GUESS_BTN_W + GUESS_BTN_GAP, GUESS_BTN_Y, GUESS_BTN_W, GUESS_BTN_H,
        "LOWER", onClick = { onGuess(HigherLowerGame.Guess.LOWER) },
    )).apply { inputEnabled = false }

    // ---- state transitions ----
    private fun startRound() {
        if (game.progress.coins < HigherLowerRules.STAKE) return
        game.progress.coins -= HigherLowerRules.STAKE
        hlGame = HigherLowerGame(SeededRng(Random.nextLong()))
        finished = false
        triggerCardPunch()
    }

    private fun onGuess(g: HigherLowerGame.Guess) {
        val hg = hlGame ?: return
        if (finished || !hg.alive) return
        handleEvents(hg.guess(g))
    }

    private fun onCashOut() {
        val hg = hlGame ?: return
        if (finished || !hg.alive) return
        val result = hg.cashOut() ?: return
        handleEvents(listOf(result))
    }

    private fun handleEvents(events: List<HigherLowerGame.Event>) {
        for (e in events) when (e) {
            is HigherLowerGame.Push -> {
                triggerCardPunch()
                pushFlashes.add(PushFlash(0f))
            }
            is HigherLowerGame.Correct -> {
                triggerCardPunch()
            }
            is HigherLowerGame.Busted -> {
                triggerCardPunch()
                triggerMomentFlash(Theme.btnRed)
                game.audio.play(Sfx.LOSE)
                game.buzz(70)
                game.progress.recordHigherLowerStreak(hlGame?.streak ?: 0)
                game.progress.markMiniGamePlayed(MiniGame.HIGHER_LOWER)
                openBustedDialog()
            }
            is HigherLowerGame.CashedOut -> {
                triggerMomentFlash(Theme.accentGreen)
                game.audio.play(Sfx.WIN)
                game.buzz(60)
                game.progress.recordHigherLowerStreak(e.streak)
                game.progress.markMiniGamePlayed(MiniGame.HIGHER_LOWER)
                openCashedOutDialog(e.payout)
            }
        }
    }

    private fun openBustedDialog() {
        finished = true
        val d = Dialog(760f, 820f, "BUSTED!", onDismiss = { game.toMiniGames() })
        d.defeat = true
        d.drawContent = { b, assets, px, py, pw, ph ->
            Ui.fitText(b, assets, "Lost your ${HigherLowerRules.STAKE}-coin stake",
                px + pw / 2f, py + ph - 700f, pw - 140f, 48f, Color.WHITE)
        }
        d.buttons += Button(d.cx - 280f, d.cy - 40f, 560f, 130f, "PLAY AGAIN", onClick = { closeDialog(); startRound() })
        d.buttons += Button(d.cx - 280f, d.cy - 200f, 560f, 130f, "BACK TO MINI GAMES", onClick = { game.toMiniGames() })
        openDialog(d)
    }

    private fun openCashedOutDialog(payout: Long) {
        finished = true
        val d = Dialog(760f, 820f, "CASHED OUT!", onDismiss = { game.toMiniGames() })
        d.fruitRain = true
        d.celebration = true
        d.drawContent = { b, assets, px, py, pw, ph ->
            Ui.fitText(b, assets, "+$payout coins", px + pw / 2f, py + ph - 1_050f, pw - 140f, 48f, Color.WHITE)
        }
        d.buttons += Button(d.cx - 350f, d.cy - 480f, 700f, 176f, "COLLECT ALL\nREWARDS!", onClick = {
            game.progress.earnCoins(payout)
            game.toMiniGames()
        })
        openDialog(d)
    }

    internal fun debugShowWinDialog() = openCashedOutDialog(120L)
    internal fun debugShowLossDialog() = openBustedDialog()

    // ---- per-frame update: no simulation to step, just keep button availability current.
    // Bails early once `finished` is set (mirrors MergeGameScreen's `finished` guard) — the
    // round-active buttons freeze in whatever state they last had, which is harmless since a
    // dialog is open by then and BaseScreen routes all input to it exclusively. ----
    override fun update(delta: Float) {
        if (finished) return
        val hg = hlGame
        val active = hg != null && hg.alive
        playBtn.enabled = !active && game.progress.coins >= HigherLowerRules.STAKE
        playBtn.inputEnabled = !active
        higherBtn.inputEnabled = active
        lowerBtn.inputEnabled = active
        cashOutBtn.inputEnabled = active
        cashOutBtn.enabled = active && (hg?.streak ?: 0) >= 1
    }

    // ---- render ----
    override fun draw(delta: Float) {
        // Juice timers MUST tick here, not in update(delta): update() bails immediately once
        // `finished` is set (see openBustedDialog()/openCashedOutDialog(), invoked from the same
        // handleEvents() call that triggers the moment-flash) — ticking there would freeze every
        // effect on frame one. draw() has no such guard; BaseScreen.render() calls it unconditionally.
        tickEffects(delta)

        Draw.cover(batch, a.fruitBackground(5))
        drawHud()
        val hg = hlGame
        if (hg != null) drawRoundUi(hg) else drawIdleUi()
        drawNavigation(backBtn, homeBtn)
        drawMomentFlash()
    }

    private fun tickEffects(delta: Float) {
        val expired = ArrayList<Int>()
        for ((key, punch) in cardPunches) {
            punch.elapsed += delta
            if (punch.elapsed >= CARD_PUNCH_DURATION) expired.add(key)
        }
        expired.forEach { cardPunches.remove(it) }

        pushFlashes.forEach { it.elapsed += delta }
        pushFlashes.removeAll { it.elapsed >= PUSH_FLASH_DURATION }

        if (momentFlashElapsed < MOMENT_FLASH_DURATION) momentFlashElapsed += delta
    }

    private fun drawHud() {
        Ui.coinCounter(batch, a, Theme.W - 320f, 2304f, "%,d".format(game.progress.coins), badgeSize = 90f, maxTextW = 180f)
        Draw.textCentered(batch, a.title, "HIGHER-LOWER", Theme.W / 2f, 2180f, Color.WHITE)
        Draw.textCentered(
            batch, a.bodyLight, "Best streak: ${game.progress.higherLowerBestStreak}",
            Theme.W / 2f, 2090f, Color.WHITE,
        )
    }

    private fun drawIdleUi() {
        Ui.button(batch, a, playBtn)
    }

    private fun drawRoundUi(hg: HigherLowerGame) {
        Draw.textCentered(batch, a.title, "STREAK ${hg.streak}", Theme.W / 2f, STREAK_LABEL_Y, Color.WHITE)
        drawCard(hg)
        drawPushFlashes()
        if (cashOutBtn.enabled) {
            Ui.fitText(batch, a, "Cash out for ${HigherLowerRules.payout(hg.streak)} coins",
                Theme.W / 2f, CASHOUT_HINT_Y, 760f, 44f, Color.WHITE)
        }
        Ui.button(batch, a, cashOutBtn)
        Ui.button(batch, a, higherBtn)
        Ui.button(batch, a, lowerBtn)
    }

    private fun drawCard(hg: HigherLowerGame) {
        Ui.panel(batch, a, Theme.W / 2f - CARD_SIZE / 2f, CARD_CENTER_Y - CARD_SIZE / 2f, CARD_SIZE, CARD_SIZE)
        var fruitSize = CARD_SIZE * 0.7f
        cardPunches[revealCounter]?.let { punch ->
            val t = (punch.elapsed / CARD_PUNCH_DURATION).coerceIn(0f, 1f)
            fruitSize *= easeOutBack(t)
        }
        Draw.imageFit(batch, a.region(hg.current), Theme.W / 2f, CARD_CENTER_Y, fruitSize, fruitSize)
    }

    private fun drawPushFlashes() {
        for (flash in pushFlashes) {
            val alpha = 1f - (flash.elapsed / PUSH_FLASH_DURATION).coerceIn(0f, 1f)
            Draw.textCentered(
                batch, a.bodyLight, "PUSH!", Theme.W / 2f, CARD_CENTER_Y + CARD_SIZE / 2f + 80f,
                Color(1f, 1f, 1f, alpha),
            )
        }
    }

    private fun drawMomentFlash() {
        if (momentFlashElapsed >= MOMENT_FLASH_DURATION) return
        val alpha = 0.5f * (1f - easeOutQuint((momentFlashElapsed / MOMENT_FLASH_DURATION).coerceIn(0f, 1f)))
        batch.setColor(momentFlashColor.r, momentFlashColor.g, momentFlashColor.b, alpha)
        batch.draw(a.white, 0f, 0f, Theme.W, Theme.H)
        batch.setColor(Color.WHITE)
    }

    private companion object {
        const val CARD_SIZE = 420f
        const val CARD_CENTER_Y = 1400f
        const val STREAK_LABEL_Y = 1750f
        const val CASHOUT_HINT_Y = 1040f

        const val PLAY_BTN_W = 640f
        const val PLAY_BTN_H = 180f
        const val PLAY_BTN_Y = CARD_CENTER_Y - PLAY_BTN_H / 2f

        const val CASHOUT_BTN_W = 560f
        const val CASHOUT_BTN_H = 140f
        const val CASHOUT_BTN_Y = 860f

        const val GUESS_BTN_W = 460f
        const val GUESS_BTN_H = 160f
        const val GUESS_BTN_GAP = 40f
        const val GUESS_BTN_Y = 650f
        const val GUESS_BTN_START_X = (Theme.W - (GUESS_BTN_W * 2f + GUESS_BTN_GAP)) / 2f

        // Juice durations (seconds).
        const val CARD_PUNCH_DURATION = 0.22f
        const val PUSH_FLASH_DURATION = 0.6f
        const val MOMENT_FLASH_DURATION = 0.45f
    }
}

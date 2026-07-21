package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import studio.cortex.fruvio.Booster
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.Sfx
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.anim.easeOutBack
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.ActionRole
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.Ui

/** Offline 2x4 booster market. Every purchase is persistent and usable inside Merge gameplay. */
class ShopScreen(game: FruvioGame) : BaseScreen(game) {
    private data class Offer(
        val booster: Booster,
        val label: String,
        val line1: String,
        val line2: String,
        val price: Long,
        val iconTier: FruitTier,
    )

    private val offers = listOf(
        Offer(Booster.EXTRA_PREVIEW, "NEXT x3", "Reveal three", "upcoming fruits", 75L, FruitTier.LEMON),
        Offer(Booster.REMOVE_FRUIT, "REMOVE", "Tap and remove", "one problem fruit", 120L, FruitTier.CHERRY),
        Offer(Booster.SHAKE_JAR, "SHAKE", "Safely remix", "the whole jar", 100L, FruitTier.WATERMELON),
        Offer(Booster.COMBO_FREEZE, "FREEZE", "Pause combo", "decay for 8 sec", 140L, FruitTier.RASPBERRY),
        Offer(Booster.REROLL_NEXT, "REROLL", "Replace the fruit", "waiting to drop", 85L, FruitTier.PEACH),
        Offer(Booster.UPGRADE_FRUIT, "UPGRADE", "Promote one fruit", "to its next tier", 180L, FruitTier.LEMON),
        Offer(Booster.OVERFLOW_SHIELD, "SHIELD", "Block overflow", "for 12 seconds", 160L, FruitTier.WATERMELON),
        Offer(Booster.DOUBLE_REWARD, "COINS x2", "Double the level", "coin reward", 220L, FruitTier.PEACH),
    )

    private val backBtn = addBackNavigation { game.toMenu() }
    private val homeBtn = addHomeNavigation()
    private val ownedPulse = HashMap<Booster, Float>()
    private val cardButtons = offers.mapIndexed { index, offer ->
        val col = index % 2
        val row = index / 2
        add(Button(
            GRID_X + col * (CARD_W + CARD_GAP_X),
            GRID_TOP - (row + 1) * CARD_H - row * CARD_GAP_Y,
            CARD_W, CARD_H,
            enabled = game.progress.coins >= offer.price,
            onClick = { buy(offer) },
        ))
    }

    private fun buy(offer: Offer) {
        if (game.progress.coins < offer.price) return
        game.progress.coins -= offer.price
        game.progress.addBooster(offer.booster)
        ownedPulse[offer.booster] = 0f
        game.audio.play(Sfx.CLICK)
        game.buzz()
    }

    override fun update(delta: Float) {
        offers.forEachIndexed { i, offer -> cardButtons[i].enabled = game.progress.coins >= offer.price }
    }

    override fun draw(delta: Float) {
        tickPulses(delta)
        Draw.cover(batch, a.fruitBackground(5))
        Ui.coinCounter(batch, a, Theme.W - 320f, 2304f, "%,d".format(game.progress.coins), badgeSize = 90f, maxTextW = 180f)
        Ui.fitText(batch, a, "BOOSTER MARKET", Theme.W / 2f, 2202f, 650f, 90f, Color.WHITE)
        Draw.textCentered(batch, a.captionLight, "BUY ONCE · USE IN ANY MERGE LEVEL", Theme.W / 2f, 2124f, Color.WHITE)
        drawNavigation(backBtn, homeBtn)
        offers.forEachIndexed { i, offer -> drawCard(offer, cardButtons[i]) }
        Draw.textCentered(batch, a.captionLight, "TAP A CARD TO BUY", Theme.W / 2f, 438f, Color.WHITE)
    }

    private fun tickPulses(delta: Float) {
        for (booster in ownedPulse.keys.toList()) {
            val time = ownedPulse.getValue(booster) + delta
            if (time >= PULSE_DURATION) ownedPulse.remove(booster) else ownedPulse[booster] = time
        }
    }

    private fun drawCard(offer: Offer, button: Button) {
        val tint = when {
            !button.enabled -> Color(0.48f, 0.48f, 0.56f, 1f)
            button.pressed -> Color(0.82f, 0.90f, 1f, 1f)
            else -> Color.WHITE
        }
        Draw.panel(batch, a.uiSkin.jarPanel, button.x, button.y, button.w, button.h, tint)
        val pulse = ownedPulse[offer.booster]
        val iconScale = if (pulse == null) 1f else 1f + PULSE_AMPLITUDE *
            (1f - easeOutBack((pulse / PULSE_DURATION).coerceIn(0f, 1f)))
        Draw.imageFit(batch, a.region(offer.iconTier), button.cx, button.y + button.h - 86f,
            84f * iconScale, 84f * iconScale)
        Ui.fitText(batch, a, offer.label, button.cx, button.y + button.h - 161f, button.w - 70f, 42f, Color.WHITE)
        // line1/line2 are one sentence wrapped across two lines — without a forced shared font
        // each line auto-fit independently, so a short first line rendered in a bigger font than
        // a longer second line and the sentence looked like two mismatched sizes stacked up.
        Ui.fitText(batch, a, offer.line1, button.cx, button.y + button.h - 209f, button.w - 58f, 34f, Color.WHITE, font = a.bodyLight)
        Ui.fitText(batch, a, offer.line2, button.cx, button.y + button.h - 245f, button.w - 58f, 34f, Color.WHITE, font = a.bodyLight)
        Ui.fitText(batch, a, "OWNED ${game.progress.boosterCount(offer.booster)}", button.x + 118f,
            button.y + 63f, 175f, 38f, Color.WHITE)
        Ui.coinCounter(batch, a, button.x + button.w - 112f, button.y + 63f, "${offer.price}", 58f, 86f)
    }

    private companion object {
        const val GRID_X = 70f
        const val GRID_TOP = 2080f
        const val CARD_W = 455f
        const val CARD_H = 375f
        const val CARD_GAP_X = 30f
        const val CARD_GAP_Y = 28f
        const val PULSE_DURATION = 0.25f
        const val PULSE_AMPLITUDE = 0.28f
    }
}
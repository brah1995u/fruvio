package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.Sfx
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.anim.easeOutBack
import studio.cortex.fruvio.engine.achievements.AchievementDef
import studio.cortex.fruvio.engine.achievements.AchievementProgress
import studio.cortex.fruvio.engine.achievements.Achievements
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.ScrollArea
import studio.cortex.fruvio.ui.Ui

/** Persistent 20-goal progression with explicit LOCKED, READY and CLAIMED states. */
class AchievementsScreen(game: FruvioGame) : BaseScreen(game) {
    private val backBtn = addBackNavigation { game.toMenu() }
    private val homeBtn = addHomeNavigation()

    private val viewX = 90f
    private val viewY = 90f
    private val viewW = Theme.W - 180f
    private val viewH = 1930f
    private val viewTop = viewY + viewH
    private val cardH = 280f
    private val cardGap = 30f

    private val defs = Achievements.all
    private val cardY = FloatArray(defs.size)
    private val scrollArea = add(ScrollArea(viewX, viewY, viewW, viewH)).apply {
        contentH = defs.size * cardH + (defs.size - 1) * cardGap
    }
    private val claimPulse = HashMap<String, Float>()
    private val claimButtons: List<Button> = defs.map { def ->
        add(Button(0f, 0f, CLAIM_W, CLAIM_H, "+${def.coinReward}", onClick = {
            if (game.progress.claimAchievement(def)) {
                claimPulse[def.id] = 0f
                game.audio.play(Sfx.WIN)
                game.buzz(60)
            }
        }))
    }

    private var snapshot = game.progress.achievementSnapshot()

    override fun update(delta: Float) {
        snapshot = game.progress.achievementSnapshot()
        defs.forEachIndexed { index, def ->
            val y = viewTop - (index + 1) * cardH - index * cardGap - scrollArea.scroll
            cardY[index] = y
            val claimed = game.progress.isAchievementClaimed(def.id)
            val complete = AchievementProgress.isComplete(def, snapshot)
            val visible = y < viewTop && y + cardH > viewY
            claimButtons[index].apply {
                x = viewX + viewW - CLAIM_PADDING - CLAIM_W
                this.y = y + (cardH - CLAIM_H) / 2f
                enabled = complete && !claimed
                inputEnabled = visible && !claimed
                label = when {
                    claimed -> "CLAIMED"
                    complete -> "CLAIM +${def.coinReward}"
                    else -> "REWARD +${def.coinReward}"
                }
                style = if (complete && !claimed) Button.Style.RED else Button.Style.PRIMARY
            }
        }
    }

    override fun draw(delta: Float) {
        tickPulses(delta)
        Draw.cover(batch, a.fruitBackground(4))
        Draw.textCentered(batch, a.title, "ACHIEVEMENTS", Theme.W / 2f, 2220f, Color.WHITE)
        Ui.fitText(batch, a, "COMPLETE GOALS - CLAIM COINS - BUY BOOSTERS", Theme.W / 2f, 2156f, 760f, 34f, Color.WHITE, font = a.captionLight)
        drawSummary()
        drawNavigation(backBtn, homeBtn)

        if (Draw.clipBegin(batch, game.camera, viewX, viewY, viewW, viewH)) {
            defs.forEachIndexed { index, def -> drawCard(def, index) }
            Draw.clipEnd(batch)
        }
    }

    private fun drawSummary() {
        val claimed = defs.count { game.progress.isAchievementClaimed(it.id) }
        val ready = defs.count {
            !game.progress.isAchievementClaimed(it.id) && AchievementProgress.isComplete(it, snapshot)
        }
        Draw.panel(batch, a.uiSkin.jarPanel, 220f, 2042f, 640f, 104f)
        Draw.imageFit(batch, a.region(FruitTier.LEMON), 258f, 2094f, 64f, 64f)
        Draw.imageFit(batch, a.region(FruitTier.RASPBERRY), 822f, 2094f, 64f, 64f)
        Ui.fitText(batch, a, "${claimed} / ${defs.size} CLAIMED  -  ${ready} READY",
            Theme.W / 2f, 2094f, 500f, 42f, Color.WHITE, font = a.titleSmall)
    }

    private fun tickPulses(delta: Float) {
        for (id in claimPulse.keys.toList()) {
            val time = claimPulse.getValue(id) + delta
            if (time >= PULSE_DURATION) claimPulse.remove(id) else claimPulse[id] = time
        }
    }

    private fun drawCard(def: AchievementDef, index: Int) {
        val y = cardY[index]
        if (y + cardH < viewY || y > viewTop) return

        val claimed = game.progress.isAchievementClaimed(def.id)
        val complete = AchievementProgress.isComplete(def, snapshot)
        val cardTint = when {
            claimed -> Color(0.72f, 0.82f, 0.76f, 1f)
            complete -> Color.WHITE
            else -> Color(0.78f, 0.78f, 0.88f, 1f)
        }
        Draw.panel(batch, a.uiSkin.jarPanel, viewX, y, viewW, cardH, cardTint)

        val fruitCx = viewX + 78f
        val textX = viewX + 142f
        val textW = viewW - 142f - CLAIM_W - CLAIM_PADDING - 28f
        val textCx = textX + textW / 2f
        val tier = FruitTier.entries[index.mod(FruitTier.entries.size)]
        if (complete && !claimed) {
            Draw.imageFitTinted(batch, a.uiSkin.orbGreen, fruitCx, y + cardH / 2f, 128f, 128f,
                Theme.lemonYellow, alpha = 0.38f)
        }
        Draw.imageFit(batch, a.region(tier), fruitCx, y + cardH / 2f, 100f, 100f,
            alpha = if (claimed) 0.72f else 1f)

        Ui.fitText(batch, a, def.title.uppercase(), textCx, y + 226f, textW, 44f, Color.WHITE, font = a.buttonLight)
        Ui.wrappedText(batch, a.captionLight, def.description, textCx, y + 168f, textW, Color.WHITE)

        val current = AchievementProgress.currentValue(def, snapshot).coerceAtLeast(0)
        val target = AchievementProgress.targetValue(def, snapshot).coerceAtLeast(1)
        val shown = current.coerceAtMost(target)
        val status = when {
            claimed -> "CLAIMED В· REWARD COLLECTED"
            complete -> "READY TO CLAIM"
            else -> "PROGRESS - $shown / $target ${def.unitLabel.uppercase()}"
        }
        Ui.fitText(batch, a, status, textCx, y + 86f, textW, 32f,
            if (complete && !claimed) Theme.lemonYellow else Color.WHITE, font = a.captionLight)
        drawProgressBar(textX, y + 46f, textW, shown.toFloat() / target.toFloat(), claimed)

        val button = claimButtons[index]
        Ui.button(batch, a, button)
        claimPulse[def.id]?.let { pulseTime ->
            val scale = 1f + PULSE_AMPLITUDE *
                (1f - easeOutBack((pulseTime / PULSE_DURATION).coerceIn(0f, 1f)))
            a.bodyLight.data.setScale(scale)
            Draw.textCentered(batch, a.bodyLight, "+${def.coinReward}", button.cx, button.cy, Color.WHITE)
            a.bodyLight.data.setScale(1f)
        }
    }

    private fun drawProgressBar(x: Float, y: Float, width: Float, progress: Float, claimed: Boolean) {
        batch.setColor(0.08f, 0.05f, 0.20f, 0.86f)
        batch.draw(a.white, x, y, width, PROGRESS_H)
        val color = if (claimed) Theme.lemonYellow else Theme.accentGreen
        batch.setColor(color.r, color.g, color.b, 1f)
        batch.draw(a.white, x, y, width * progress.coerceIn(0f, 1f), PROGRESS_H)
        batch.setColor(Color.WHITE)
    }

    private companion object {
        const val CLAIM_W = 255f
        const val CLAIM_H = 150f
        const val CLAIM_PADDING = 38f
        const val PROGRESS_H = 18f
        const val PULSE_DURATION = 0.25f
        const val PULSE_AMPLITUDE = 0.35f
    }
}
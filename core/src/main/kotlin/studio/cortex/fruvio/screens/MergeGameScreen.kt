package studio.cortex.fruvio.screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import studio.cortex.fruvio.Booster
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.Sfx
import studio.cortex.fruvio.Theme
import studio.cortex.fruvio.anim.easeOutBack
import studio.cortex.fruvio.anim.easeOutCubic
import studio.cortex.fruvio.anim.easeOutQuint
import studio.cortex.fruvio.engine.merge.CocktailCombo
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.engine.merge.LevelDef
import studio.cortex.fruvio.engine.merge.MergeGame
import studio.cortex.fruvio.engine.merge.MergeRules
import studio.cortex.fruvio.engine.merge.SeededRng
import studio.cortex.fruvio.engine.merge.StarRating
import studio.cortex.fruvio.engine.merge.WinCondition
import studio.cortex.fruvio.physics.MergeWorld
import studio.cortex.fruvio.render.Draw
import studio.cortex.fruvio.ui.ActionRole
import studio.cortex.fruvio.ui.BaseScreen
import studio.cortex.fruvio.ui.Button
import studio.cortex.fruvio.ui.Dialog
import studio.cortex.fruvio.ui.Ui
import kotlin.random.Random

/** Physics-driven merge level with deterministic stepping and eight persistent gameplay boosters. */
class MergeGameScreen(game: FruvioGame, private val level: LevelDef) : BaseScreen(game) {
    private val mergeGame = MergeGame(level, SeededRng(Random.nextLong()))
    private val mergeWorld = MergeWorld(level)

    private val pixelsPerMeter = run {
        val maxW = Theme.W - PLAY_AREA_SIDE_MARGIN * 2f - JAR_FRAME_PAD * 2f
        val maxH = HUD_BOTTOM_Y - PLAY_AREA_BOTTOM - JAR_FRAME_PAD * 2f
        minOf(maxW / MAX_CAMPAIGN_JAR_WIDTH, maxH / MAX_CAMPAIGN_JAR_HEIGHT)
    }
    private val jarWidthPx = level.jarWidthUnits * pixelsPerMeter
    private val jarHeightPx = level.jarHeightUnits * pixelsPerMeter
    private val jarOriginX = (Theme.W - jarWidthPx) / 2f
    private val jarOriginY = PLAY_AREA_BOTTOM + JAR_FRAME_PAD
    private val spoutScreenY = jarOriginY + jarHeightPx * SPOUT_HEIGHT_FRACTION

    private var spoutX = level.jarWidthUnits / 2f
    private var dragging = false
    private var finished = false
    private var physicsAccumulator = 0f
    private var overflowSuppressionSeconds = 0f
    private var shieldSeconds = 0f
    private val cocktailCombo = CocktailCombo()
    private var comboPulseSeconds = 0f
    private var lastComboMultiplier = 1
    private var lastComboBonus = 0
    private var previewActive = false
    private var removeTargeting = false
    private var upgradeTargeting = false
    private var rewardBoostActive = false
    private var debugScoreOverride: Int? = null

    private data class SpawnPunch(var elapsed: Float, val kind: Kind) { enum class Kind { DROP, MERGE } }
    private val spawnPunches = HashMap<Long, SpawnPunch>()
    private data class MergeBurst(val tier: FruitTier, val x: Float, val y: Float, var elapsed: Float)
    private val mergeBursts = ArrayList<MergeBurst>()
    private var momentFlashElapsed = Float.MAX_VALUE
    private var momentFlashColor: Color = Color.WHITE
    private var shakeElapsed = Float.MAX_VALUE

    private val backBtn = addBackNavigation { game.toLevelSelect() }
    private val homeBtn = addHomeNavigation()

    /** No chrome at all for this row (see [drawBoosterRow]) — just a fruit + an existing-icon
     *  badge/overlay per booster, reusing shipped assets instead of commissioning new art. */
    private data class BoosterVisual(
        val fruit: FruitTier,
        val badge: TextureRegion? = null,
        val badgeRotation: Float = 0f,
        /** OVERFLOW_SHIELD: a tinted orb behind the fruit, larger — a shield-bubble glow. */
        val glowBehind: Boolean = false,
        /** COMBO_FREEZE: a tinted orb on top of the fruit, translucent — a frosted-dome look. */
        val frostOverlay: Boolean = false,
    )

    private val boosterKinds = Booster.entries.toList()
    private val boosterLabels = listOf("NEXT x3", "REMOVE", "SHAKE", "FREEZE", "REROLL", "UPGRADE", "SHIELD", "COINS x2")
    // Each label used to auto-scale to fit its own width independently — short words like "SHAKE"
    // rendered near full titleSmall size while longer ones like "NEXT x3"/"COINS x2" shrank to
    // fit the same 120px slot, so the row looked like 8 different font sizes side by side.
    // Pinning every label to the scale the *longest* one needs keeps the whole row uniform.
    private val boosterLabelScale = boosterLabels.minOf {
        Ui.textFitScale(game.assets.titleSmall, it, BOOSTER_SLOT_W * 0.92f, 34f)
    }
    private val boosterVisuals = listOf(
        BoosterVisual(FruitTier.LEMON, badge = game.assets.iconArrow),                             // EXTRA_PREVIEW
        BoosterVisual(FruitTier.RASPBERRY, badge = game.assets.iconClose),                          // REMOVE_FRUIT
        BoosterVisual(FruitTier.WATERMELON),                                                        // SHAKE_JAR
        BoosterVisual(FruitTier.CHERRY, frostOverlay = true),                                       // COMBO_FREEZE
        BoosterVisual(FruitTier.PEACH, badge = game.assets.iconArrow, badgeRotation = 180f),        // REROLL_NEXT
        BoosterVisual(FruitTier.LEMON, badge = game.assets.iconStar),                               // UPGRADE_FRUIT
        BoosterVisual(FruitTier.WATERMELON, glowBehind = true),                                     // OVERFLOW_SHIELD
        BoosterVisual(FruitTier.PEACH, badge = game.assets.uiSkin.coinBadge),                        // DOUBLE_REWARD
    )
    private val boosterButtons = boosterKinds.mapIndexed { index, booster ->
        add(Button(
            BOOSTER_ROW_START_X + index * BOOSTER_SLOT_W, BOOSTER_ROW_BOTTOM_Y,
            BOOSTER_SLOT_W, BOOSTER_ROW_H,
            onClick = { activateBooster(booster) },
        ))
    }

    override fun update(delta: Float) {
        updateBoosterButtons()
        if (finished) return
        val frameDelta = delta.coerceIn(0f, MAX_FRAME_DELTA)
        cocktailCombo.advance(frameDelta)
        comboPulseSeconds = (comboPulseSeconds - frameDelta).coerceAtLeast(0f)
        overflowSuppressionSeconds = (overflowSuppressionSeconds - frameDelta).coerceAtLeast(0f)
        shieldSeconds = (shieldSeconds - frameDelta).coerceAtLeast(0f)
        physicsAccumulator += frameDelta
        var steps = 0
        while (physicsAccumulator >= FIXED_STEP && steps < MAX_STEPS_PER_FRAME && !finished) {
            mergeWorld.step(FIXED_STEP)
            physicsAccumulator -= FIXED_STEP
            steps++
            resolvePendingMerges()
            if (!finished) {
                val overflowing = overflowSuppressionSeconds <= 0f && mergeWorld.isOverflowing()
                handleEvents(mergeGame.tickOverflow(overflowing, FIXED_STEP))
            }
        }
        if (steps == MAX_STEPS_PER_FRAME) physicsAccumulator = physicsAccumulator.coerceAtMost(FIXED_STEP)
    }

    private fun updateBoosterButtons() {
        boosterKinds.forEachIndexed { index, booster ->
            val available = game.progress.boosterCount(booster) > 0
            val hasFruit = mergeWorld.liveFruitBodies().isNotEmpty()
            val stateAllowsUse = when (booster) {
                Booster.EXTRA_PREVIEW -> !previewActive
                Booster.REMOVE_FRUIT, Booster.SHAKE_JAR, Booster.UPGRADE_FRUIT -> hasFruit
                Booster.COMBO_FREEZE -> cocktailCombo.streak > 0 && cocktailCombo.freezeRemainingSeconds <= 0f
                Booster.OVERFLOW_SHIELD -> shieldSeconds <= 0f
                Booster.DOUBLE_REWARD -> !rewardBoostActive
                else -> true
            }
            boosterButtons[index].enabled = !finished && available && stateAllowsUse
        }
    }

    private fun activateBooster(booster: Booster) = when (booster) {
        Booster.EXTRA_PREVIEW -> activatePreview()
        Booster.REMOVE_FRUIT -> toggleRemove()
        Booster.SHAKE_JAR -> activateShake()
        Booster.COMBO_FREEZE -> activateFreeze()
        Booster.REROLL_NEXT -> activateReroll()
        Booster.UPGRADE_FRUIT -> toggleUpgrade()
        Booster.OVERFLOW_SHIELD -> activateShield()
        Booster.DOUBLE_REWARD -> activateDoubleReward()
    }

    private fun activatePreview() {
        if (finished || previewActive) return
        if (game.progress.consumeBooster(Booster.EXTRA_PREVIEW)) previewActive = true
    }

    private fun toggleRemove() {
        if (finished || game.progress.boosterCount(Booster.REMOVE_FRUIT) <= 0) return
        removeTargeting = !removeTargeting
        if (removeTargeting) upgradeTargeting = false
    }

    private fun activateShake() {
        if (finished || !game.progress.consumeBooster(Booster.SHAKE_JAR)) return
        mergeWorld.shake()
        overflowSuppressionSeconds = maxOf(overflowSuppressionSeconds, SHAKE_OVERFLOW_PROTECTION)
        mergeGame.resetOverflowGrace()
        shakeElapsed = 0f
        game.buzz(35)
    }

    private fun activateFreeze() {
        if (finished || !game.progress.consumeBooster(Booster.COMBO_FREEZE)) return
        cocktailCombo.freeze(CocktailCombo.FREEZE_DURATION_SECONDS)
        game.buzz(25)
    }

    private fun activateReroll() {
        if (finished || !game.progress.consumeBooster(Booster.REROLL_NEXT)) return
        mergeGame.spawnQueue.rerollCurrent()
        game.audio.play(Sfx.CLICK)
        game.buzz(18)
    }

    private fun toggleUpgrade() {
        if (finished || game.progress.boosterCount(Booster.UPGRADE_FRUIT) <= 0) return
        upgradeTargeting = !upgradeTargeting
        if (upgradeTargeting) removeTargeting = false
    }

    private fun activateShield() {
        if (finished || !game.progress.consumeBooster(Booster.OVERFLOW_SHIELD)) return
        shieldSeconds = SHIELD_DURATION
        overflowSuppressionSeconds = maxOf(overflowSuppressionSeconds, shieldSeconds)
        mergeGame.resetOverflowGrace()
        game.buzz(30)
    }

    private fun activateDoubleReward() {
        if (finished || rewardBoostActive || !game.progress.consumeBooster(Booster.DOUBLE_REWARD)) return
        rewardBoostActive = true
        game.audio.play(Sfx.WIN, 0.45f)
        game.buzz(30)
    }

    private data class FruitSnapshot(val tier: FruitTier, val id: Long, val x: Float, val y: Float)

    private fun resolvePendingMerges() {
        val pending = mergeWorld.drainPendingMerges()
        if (pending.isEmpty()) return
        val snapshots = pending.flatMap { (a, b) -> listOf(a, b) }.distinct().mapNotNull { body ->
            (body.userData as? MergeWorld.FruitBodyData)?.let { data ->
                body to FruitSnapshot(data.tier, data.id, body.position.x, body.position.y)
            }
        }.toMap()
        val consumed = HashSet<Long>()
        for ((bodyA, bodyB) in pending) {
            val dataA = snapshots[bodyA] ?: continue
            val dataB = snapshots[bodyB] ?: continue
            if (dataA.id in consumed || dataB.id in consumed) continue
            val resultTier = MergeRules.resolve(dataA.tier, dataB.tier) ?: continue
            consumed += dataA.id
            consumed += dataB.id
            mergeWorld.removeFruit(bodyA)
            mergeWorld.removeFruit(bodyB)
            val midX = (dataA.x + dataB.x) / 2f
            val midY = (dataA.y + dataB.y) / 2f
            val resultBody = mergeWorld.spawnFruit(resultTier, midX, midY)
            spawnPunches.remove(dataA.id)
            spawnPunches.remove(dataB.id)
            spawnPunches[(resultBody.userData as MergeWorld.FruitBodyData).id] = SpawnPunch(0f, SpawnPunch.Kind.MERGE)
            mergeBursts.add(MergeBurst(resultTier, midX, midY, 0f))
            val award = cocktailCombo.recordMerge()
            lastComboMultiplier = award.multiplier
            lastComboBonus = award.bonus
            comboPulseSeconds = COMBO_PULSE_SECONDS
            handleEvents(mergeGame.reportMerge(resultTier, award.multiplier, award.bonus, award.streak))
            if (finished) return
        }
    }

    private fun handleEvents(events: List<MergeGame.Event>) {
        for (event in events) when (event) {
            is MergeGame.Merged -> {
                game.audio.play(Sfx.MERGE)
                game.buzz(20)
                game.progress.recordMerges()
                if (event.comboBonus > 0) { game.audio.play(Sfx.WIN, 0.35f); game.buzz(35) }
            }
            is MergeGame.TierReached -> { game.buzz(35); game.progress.recordTierReached(event.tier) }
            MergeGame.TargetMet -> {
                triggerMomentFlash(Theme.accentGreen)
                game.audio.play(Sfx.WIN)
                game.buzz(60)
                openVictoryDialog()
            }
            MergeGame.DropLimitExceeded -> {
                triggerMomentFlash(Theme.btnRed)
                game.audio.play(Sfx.LOSE)
                game.buzz(70)
                openDefeatDialog("Out of drops!")
            }
            MergeGame.Overflowed -> {
                triggerMomentFlash(Theme.btnRed)
                shakeElapsed = 0f
                game.audio.play(Sfx.LOSE)
                game.buzz(70)
                openDefeatDialog("The jar overflowed!")
            }
        }
    }

    private fun triggerMomentFlash(color: Color) {
        momentFlashElapsed = 0f
        momentFlashColor = color
    }

    private fun openVictoryDialog() {
        if (finished) return
        finished = true
        dragging = false
        removeTargeting = false
        upgradeTargeting = false
        val starsEarned = StarRating.stars(level, mergeGame.dropsUsed)
        val reward = level.coinReward * if (rewardBoostActive) 2 else 1
        game.progress.recordStars(level.index, starsEarned)
        val d = Dialog(760f, 900f, "LEVEL CLEARED!", onDismiss = { game.toLevelSelect() })
        d.fruitRain = true
        d.celebration = true
        d.drawContent = { b, assets, px, py, pw, ph ->
            for (s in 0 until 3) {
                // Spread across the tier range (CHERRY, LEMON, WATERMELON) instead of adjacent
                // tiers, so the three reward slots stay visually distinct at a glance.
                val tier = FruitTier.entries[s * 2]
                Draw.imageFit(b, assets.region(tier), px + pw / 2f - 180f + s * 180f, py + ph - 920f, 104f, 104f,
                    alpha = if (s < starsEarned) 1f else 0.25f)
            }
            Draw.textCentered(b, assets.bodyLight, "+$reward coins", px + pw / 2f, py + ph - 970f, Color.WHITE)
        }
        d.buttons += Button(d.cx - 350f, d.cy - 450f, 700f, 176f, "COLLECT ALL\nREWARDS!", onClick = {
            game.progress.earnCoins(reward.toLong())
            game.progress.grantPlinkoTickets(PLINKO_TICKETS_PER_LEVEL)
            game.toLevelSelect()
        })
        openDialog(d)
    }

    internal fun debugShowWinDialog() = openVictoryDialog()
    internal fun debugShowLossDialog() = openDefeatDialog("The jar overflowed!")

    internal fun debugConfigureHud(score: Int, extraPreview: Boolean) {
        debugScoreOverride = score.coerceAtLeast(0)
        previewActive = extraPreview
    }

    private fun openDefeatDialog(reason: String) {
        if (finished) return
        finished = true
        dragging = false
        removeTargeting = false
        upgradeTargeting = false
        val d = Dialog(760f, 820f, "LEVEL FAILED", onDismiss = { game.toLevelSelect() })
        d.defeat = true
        d.drawContent = { b, assets, px, py, pw, ph ->
            Ui.fitText(b, assets, reason, px + pw / 2f, py + ph - 700f, pw - 150f, 48f, Color.WHITE)
        }
        d.buttons += Button(d.cx - 280f, d.cy - 40f, 560f, 130f, "RETRY", onClick = { game.toMerge(level) })
        d.buttons += Button(d.cx - 280f, d.cy - 200f, 560f, 130f, "BACK TO LEVELS", onClick = { game.toLevelSelect() })
        openDialog(d)
    }

    override fun onTouchDown(x: Float, y: Float): Boolean {
        if (finished) return true
        if (removeTargeting || upgradeTargeting) {
            val worldX = (x - jarOriginX) / pixelsPerMeter
            val worldY = (y - jarOriginY) / pixelsPerMeter
            val body = mergeWorld.findFruitAt(worldX, worldY)
            if (body != null && removeTargeting && game.progress.consumeBooster(Booster.REMOVE_FRUIT)) {
                val data = body.userData as MergeWorld.FruitBodyData
                spawnPunches.remove(data.id)
                mergeWorld.removeFruit(body)
                removeTargeting = false
                mergeGame.resetOverflowGrace()
                game.audio.play(Sfx.MERGE, 0.55f)
                game.buzz(25)
            } else if (body != null && upgradeTargeting) {
                val data = body.userData as MergeWorld.FruitBodyData
                val upgraded = data.tier.next
                if (upgraded != null && game.progress.consumeBooster(Booster.UPGRADE_FRUIT)) {
                    val bx = body.position.x
                    val by = body.position.y
                    spawnPunches.remove(data.id)
                    mergeWorld.removeFruit(body)
                    val result = mergeWorld.spawnFruit(upgraded, bx, by)
                    spawnPunches[(result.userData as MergeWorld.FruitBodyData).id] = SpawnPunch(0f, SpawnPunch.Kind.MERGE)
                    mergeBursts += MergeBurst(upgraded, bx, by, 0f)
                    upgradeTargeting = false
                    mergeGame.resetOverflowGrace()
                    game.audio.play(Sfx.MERGE)
                    game.buzz(35)
                    handleEvents(mergeGame.reportTierBoost(upgraded))
                }
            }
            return true
        }
        if (y > HUD_BOTTOM_Y || y < PLAY_AREA_BOTTOM) return false
        dragging = true
        updateSpout(x)
        return true
    }

    override fun onTouchDragged(x: Float, y: Float): Boolean {
        if (finished || !dragging) return false
        updateSpout(x)
        return true
    }

    override fun onTouchUp(x: Float, y: Float): Boolean {
        if (finished || !dragging) return false
        dragging = false
        dropCurrent()
        return true
    }

    private fun updateSpout(screenX: Float) {
        val r = mergeWorld.radiusMeters(mergeGame.spawnQueue.current)
        val worldX = (screenX - jarOriginX) / pixelsPerMeter
        spoutX = worldX.coerceIn(r, level.jarWidthUnits - r)
    }

    private fun dropCurrent() {
        if (finished || mergeGame.won || mergeGame.lost) return
        val tier = mergeGame.spawnQueue.current
        // Validate the drop budget before creating a Box2D body. The previous order spawned a
        // phantom 19th fruit and then immediately opened the "out of drops" dialog.
        handleEvents(mergeGame.dropCurrent())
        if (finished) return
        val body = mergeWorld.spawnFruit(tier, spoutX, level.jarHeightUnits * SPOUT_HEIGHT_FRACTION)
        spawnPunches[(body.userData as MergeWorld.FruitBodyData).id] = SpawnPunch(0f, SpawnPunch.Kind.DROP)
        game.audio.play(Sfx.DROP)
        game.buzz(15)
    }

    internal fun debugSeedCombo(streak: Int) {
        require(streak >= 1)
        repeat(streak) {
            val award = cocktailCombo.recordMerge()
            lastComboMultiplier = award.multiplier
            lastComboBonus = award.bonus
            mergeGame.reportMerge(FruitTier.CHERRY, award.multiplier, award.bonus, award.streak)
        }
        comboPulseSeconds = COMBO_PULSE_SECONDS
    }
    internal fun debugSeedDemo() {
        for (fraction in listOf(0.25f, 0.5f, 0.75f)) {
            val r = mergeWorld.radiusMeters(mergeGame.spawnQueue.current)
            spoutX = (level.jarWidthUnits * fraction).coerceIn(r, level.jarWidthUnits - r)
            dropCurrent()
        }
    }

    override fun draw(delta: Float) {
        tickEffects(delta)
        Draw.cover(batch, a.fruitBackground(level.index))

        drawHud()
        drawJar()
        drawFruits()
        drawMergeBursts()
        if (!finished) drawSpoutPreview()
        drawNavigation(backBtn, homeBtn)
        drawBoosterRow()
        drawMomentFlash()
    }

    /** No panel/chrome for these 8 slots at all — just a fruit icon (+ badge/overlay per
     *  [boosterVisuals]) and two lines of text, floating directly on the background. Sits in a
     *  fixed band anchored to [HUD_BOTTOM_Y] rather than the jar, since jar height varies by
     *  level but this needs to clear every level's jar top edge the same way. */
    private fun drawBoosterRow() {
        val iconCy = BOOSTER_ROW_BOTTOM_Y + BOOSTER_ROW_H * 0.70f
        val labelCy = BOOSTER_ROW_BOTTOM_Y + BOOSTER_ROW_H * 0.32f
        val countCy = BOOSTER_ROW_BOTTOM_Y + BOOSTER_ROW_H * 0.08f
        val states = boosterKinds.map { boosterState(it) }
        // "TARGET"/"12s" are wider than "x1"/"ON" — without a shared scale the row's state line
        // would show the same per-sibling size mismatch the label line just got fixed for.
        val stateScale = states.minOf { Ui.textFitScale(a.captionLight, it, BOOSTER_SLOT_W * 0.92f, 26f) }
        boosterKinds.forEachIndexed { index, booster ->
            val button = boosterButtons[index]
            val visual = boosterVisuals[index]
            val alpha = if (button.enabled) 1f else 0.5f

            if (visual.glowBehind) {
                Draw.imageFitTinted(batch, a.uiSkin.orbGreen, button.cx, iconCy,
                    BOOSTER_ICON_SIZE * 1.5f, BOOSTER_ICON_SIZE * 1.5f, Color(0.35f, 0.55f, 0.95f, 0.85f * alpha))
            }
            Draw.imageFit(batch, a.region(visual.fruit), button.cx, iconCy, BOOSTER_ICON_SIZE, BOOSTER_ICON_SIZE, alpha = alpha)
            if (visual.frostOverlay) {
                Draw.imageFitTinted(batch, a.uiSkin.orbGreen, button.cx, iconCy,
                    BOOSTER_ICON_SIZE * 1.15f, BOOSTER_ICON_SIZE * 1.15f, Color(0.75f, 0.92f, 1f, 0.45f * alpha))
            }
            visual.badge?.let { badge ->
                val badgeCx = button.cx + BOOSTER_ICON_SIZE * 0.34f
                val badgeCy = iconCy + BOOSTER_ICON_SIZE * 0.34f
                if (visual.badgeRotation != 0f) {
                    batch.setColor(1f, 1f, 1f, alpha)
                    Draw.imageFitRotated(batch, badge, badgeCx, badgeCy, BOOSTER_BADGE_SIZE, BOOSTER_BADGE_SIZE, visual.badgeRotation)
                    batch.setColor(Color.WHITE)
                } else {
                    Draw.imageFit(batch, badge, badgeCx, badgeCy, BOOSTER_BADGE_SIZE, BOOSTER_BADGE_SIZE, alpha = alpha)
                }
            }

            val labelColor = if (button.enabled) Color.WHITE else Color(1f, 1f, 1f, 0.55f)
            Ui.fitText(batch, a, boosterLabels[index], button.cx, labelCy, BOOSTER_SLOT_W * 0.92f, 34f,
                labelColor, font = a.titleSmall, scale = boosterLabelScale)

            val state = states[index]
            val active = state == "ON" || state == "TARGET" || state.endsWith("s")
            Ui.fitText(batch, a, state, button.cx, countCy, BOOSTER_SLOT_W * 0.92f, 26f,
                if (active) Theme.accentGreen else labelColor, font = a.captionLight, scale = stateScale)
        }
        val instruction = when {
            removeTargeting -> "TAP A FRUIT TO REMOVE"
            upgradeTargeting -> "TAP A FRUIT TO UPGRADE"
            else -> null
        }
        instruction?.let { Ui.fitText(batch, a, it, Theme.W / 2f, 286f, 760f, 38f, Color.WHITE) }
    }

    private fun boosterState(booster: Booster): String = when {
        booster == Booster.EXTRA_PREVIEW && previewActive -> "ON"
        booster == Booster.REMOVE_FRUIT && removeTargeting -> "TARGET"
        booster == Booster.UPGRADE_FRUIT && upgradeTargeting -> "TARGET"
        booster == Booster.OVERFLOW_SHIELD && shieldSeconds > 0f -> "${shieldSeconds.toInt() + 1}s"
        booster == Booster.DOUBLE_REWARD && rewardBoostActive -> "ON"
        else -> "x${game.progress.boosterCount(booster)}"
    }

    private fun tickEffects(delta: Float) {
        val expired = ArrayList<Long>()
        for ((id, punch) in spawnPunches) {
            punch.elapsed += delta
            val duration = if (punch.kind == SpawnPunch.Kind.DROP) DROP_PUNCH_DURATION else MERGE_PUNCH_DURATION
            if (punch.elapsed >= duration) expired.add(id)
        }
        expired.forEach { spawnPunches.remove(it) }
        mergeBursts.forEach { it.elapsed += delta }
        mergeBursts.removeAll { it.elapsed >= MERGE_BURST_DURATION }
        if (momentFlashElapsed < MOMENT_FLASH_DURATION) momentFlashElapsed += delta
        if (shakeElapsed < SHAKE_DURATION) shakeElapsed += delta
    }

    private fun shakeOffsetX(): Float {
        if (shakeElapsed >= SHAKE_DURATION) return 0f
        val decay = 1f - shakeElapsed / SHAKE_DURATION
        return SHAKE_MAGNITUDE * decay * MathUtils.sin(shakeElapsed * SHAKE_FREQUENCY)
    }

    private fun shakeOffsetY(): Float {
        if (shakeElapsed >= SHAKE_DURATION) return 0f
        val decay = 1f - shakeElapsed / SHAKE_DURATION
        return SHAKE_MAGNITUDE * decay * MathUtils.cos(shakeElapsed * SHAKE_FREQUENCY)
    }

    private fun drawHud() {
        Ui.coinCounter(batch, a, Theme.W - 280f, 2304f, "%,d".format(game.progress.coins), badgeSize = 70f, maxTextW = 100f)

        // Fixed score emblem: the fruit is no longer confused with the live Next queue.
        Draw.imageFit(batch, a.region(FruitTier.RASPBERRY), SCORE_FRUIT_X, HUD_MAIN_Y, 84f, 84f)
        Ui.fitText(batch, a, "${displayedScore()}", SCORE_VALUE_X, HUD_MAIN_Y,
            SCORE_VALUE_W, 88f, Color.WHITE, font = a.title)

        // Next owns a separate, bounded slot between Back and Score. Three-ahead preview scales
        // down as a group, so neither state can collide with the score emblem or navigation.
        val preview = mergeGame.spawnQueue.peekNext(if (previewActive) 3 else 1)
        val previewSize = if (previewActive) 52f else 72f
        val previewTotalW = preview.size * previewSize + (preview.size - 1) * NEXT_GAP
        val previewStartX = NEXT_CENTER_X - previewTotalW / 2f + previewSize / 2f
        preview.forEachIndexed { index, tier ->
            Draw.imageFit(batch, a.region(tier), previewStartX + index * (previewSize + NEXT_GAP),
                HUD_MAIN_Y, previewSize, previewSize)
        }
        Ui.fitText(batch, a, "NEXT", NEXT_CENTER_X, HUD_SUB_Y, 150f, 28f,
            Color.WHITE, font = a.captionLight)
        Ui.fitText(batch, a, goalText(), GOAL_CENTER_X, HUD_SUB_Y, 440f, 36f, Color.WHITE)
        drawCocktailCombo()
    }

    private fun displayedScore(): Int = debugScoreOverride ?: mergeGame.score
    private fun drawCocktailCombo() {
        if (cocktailCombo.remainingSeconds <= 0f && cocktailCombo.freezeRemainingSeconds <= 0f) return
        val alpha = if (cocktailCombo.remainingSeconds > 0f) {
            (cocktailCombo.remainingSeconds / CocktailCombo.WINDOW_SECONDS).coerceIn(0.35f, 1f)
        } else 0.5f
        val size = if (comboPulseSeconds > 0f) 82f else 70f
        // y=1755-1841 sits in the dead band between the jar's tallest top edge (1659, see
        // BOOSTER_ROW_BOTTOM_Y's comment) and the booster row's bottom (1950) on every level —
        // this used to sit at 2164-2221, right on top of the goalText row, and for a
        // ComboChallenge level (whose goal text is itself "Combo N/M · Score ...") the two
        // "combo" strings visually merged into unreadable overlapping text.
        Draw.imageFit(batch, a.uiSkin.orbGreen, 120f, 1800f, size, size, alpha = alpha)
        Draw.textCentered(batch, a.captionLight, "COMBO ${cocktailCombo.streak}  x$lastComboMultiplier", 292f, 1820f, Color(1f, 1f, 1f, alpha))
        val detail = when {
            cocktailCombo.freezeRemainingSeconds > 0f -> "FROZEN ${"%.1f".format(cocktailCombo.freezeRemainingSeconds)}s"
            lastComboBonus > 0 -> "+$lastComboBonus BONUS"
            else -> "${"%.1f".format(cocktailCombo.remainingSeconds)}s"
        }
        Draw.textCentered(batch, a.captionLight, detail, 292f, 1784f, Color(1f, 0.9f, 0.25f, alpha))
        val timerWidth = 240f * (cocktailCombo.remainingSeconds / CocktailCombo.WINDOW_SECONDS).coerceIn(0f, 1f)
        batch.setColor(Theme.accentGreen)
        batch.draw(a.white, 172f, 1755f, timerWidth, 8f)
        batch.setColor(Color.WHITE)
    }

    private fun goalText(): String = when (val wc = level.winCondition) {
        is WinCondition.ReachTier -> "Reach ${wc.target.name.lowercase().replaceFirstChar { it.uppercase() }}"
        is WinCondition.ScoreThreshold -> "Score ${displayedScore()} / ${wc.target}"
        is WinCondition.DropLimit -> "Score ${displayedScore()}/${wc.scoreThreshold} · Drops ${mergeGame.dropsUsed}/${wc.maxDrops}"
        is WinCondition.ComboChallenge -> "Combo ${mergeGame.bestCombo}/${wc.minStreak} · Score ${displayedScore()}/${wc.scoreTarget}"
    }

    private fun drawJar() {
        Draw.panel(batch, a.uiSkin.jarPanel,
            jarOriginX - JAR_FRAME_PAD + shakeOffsetX(), jarOriginY - JAR_FRAME_PAD + shakeOffsetY(),
            jarWidthPx + JAR_FRAME_PAD * 2f, jarHeightPx + JAR_FRAME_PAD * 2f)
    }

    private fun drawFruits() {
        for (body in mergeWorld.liveFruitBodies()) {
            val data = body.userData as MergeWorld.FruitBodyData
            var diameterPx = mergeWorld.radiusMeters(data.tier) * 2f * pixelsPerMeter
            spawnPunches[data.id]?.let { punch ->
                val duration = if (punch.kind == SpawnPunch.Kind.DROP) DROP_PUNCH_DURATION else MERGE_PUNCH_DURATION
                val t = (punch.elapsed / duration).coerceIn(0f, 1f)
                diameterPx *= if (punch.kind == SpawnPunch.Kind.DROP) easeOutCubic(t) else easeOutBack(t)
            }
            val screenX = jarOriginX + body.position.x * pixelsPerMeter + shakeOffsetX()
            val screenY = jarOriginY + body.position.y * pixelsPerMeter + shakeOffsetY()
            Draw.imageFitRotated(batch, a.region(data.tier), screenX, screenY, diameterPx, diameterPx,
                body.angle * MathUtils.radiansToDegrees)
        }
    }

    private fun drawMergeBursts() {
        for (burst in mergeBursts) {
            val t = (burst.elapsed / MERGE_BURST_DURATION).coerceIn(0f, 1f)
            val progress = easeOutCubic(t)
            val base = mergeWorld.radiusMeters(burst.tier) * 2f * pixelsPerMeter
            Draw.imageFit(batch, a.uiSkin.coinBadge, jarOriginX + burst.x * pixelsPerMeter,
                jarOriginY + burst.y * pixelsPerMeter, base * (1f + progress), base * (1f + progress),
                alpha = 0.45f * (1f - progress))
        }
    }

    private fun drawSpoutPreview() {
        val tier = mergeGame.spawnQueue.current
        val diameter = mergeWorld.radiusMeters(tier) * 2f * pixelsPerMeter
        Draw.imageFit(batch, a.region(tier), jarOriginX + spoutX * pixelsPerMeter + shakeOffsetX(),
            spoutScreenY + shakeOffsetY(), diameter, diameter, alpha = 0.88f)
    }

    private fun drawMomentFlash() {
        if (momentFlashElapsed >= MOMENT_FLASH_DURATION) return
        val alpha = 0.5f * (1f - easeOutQuint((momentFlashElapsed / MOMENT_FLASH_DURATION).coerceIn(0f, 1f)))
        batch.setColor(momentFlashColor.r, momentFlashColor.g, momentFlashColor.b, alpha)
        batch.draw(a.white, 0f, 0f, Theme.W, Theme.H)
        batch.setColor(Color.WHITE)
    }

    override fun hide() { mergeWorld.dispose() }
    override fun dispose() { mergeWorld.dispose() }

    private companion object {
        const val MAX_CAMPAIGN_JAR_WIDTH = 7f
        const val MAX_CAMPAIGN_JAR_HEIGHT = 10f
        const val FIXED_STEP = 1f / 60f
        const val MAX_FRAME_DELTA = 0.10f
        const val MAX_STEPS_PER_FRAME = 5
        const val SHAKE_OVERFLOW_PROTECTION = 1.25f
        const val SHIELD_DURATION = 12f
        // Single row of 8, top of screen, anchored to HUD_BOTTOM_Y rather than the jar — jar top
        // edge varies 1399-1659px across levels, but this band (1950-2125) clears all of them
        // with margin while staying below the existing top HUD (which starts at HUD_BOTTOM_Y).
        const val BOOSTER_ROW_START_X = 60f
        const val BOOSTER_ROW_BOTTOM_Y = 1950f
        const val BOOSTER_SLOT_W = 120f
        const val BOOSTER_ROW_H = 175f
        const val BOOSTER_ICON_SIZE = 76f
        const val BOOSTER_BADGE_SIZE = 34f
        const val SPOUT_HEIGHT_FRACTION = 0.95f
        const val HUD_BOTTOM_Y = 2130f
        const val HUD_MAIN_Y = 2290f
        const val HUD_SUB_Y = 2208f
        const val NEXT_CENTER_X = 270f
        const val NEXT_GAP = 6f
        const val SCORE_FRUIT_X = 455f
        const val SCORE_VALUE_X = 590f
        const val SCORE_VALUE_W = 165f
        const val GOAL_CENTER_X = 600f
        const val PLAY_AREA_BOTTOM = 310f
        const val PLAY_AREA_SIDE_MARGIN = 60f
        const val JAR_FRAME_PAD = 26f
        const val DROP_PUNCH_DURATION = 0.12f
        const val MERGE_PUNCH_DURATION = 0.22f
        const val MERGE_BURST_DURATION = 0.28f
        const val MOMENT_FLASH_DURATION = 0.45f
        const val SHAKE_DURATION = 0.35f
        const val SHAKE_MAGNITUDE = 12f
        const val SHAKE_FREQUENCY = 40f
        const val COMBO_PULSE_SECONDS = 0.32f
        const val PLINKO_TICKETS_PER_LEVEL = 3
    }
}
package studio.cortex.fruvio

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import studio.cortex.fruvio.engine.merge.LevelDef
import studio.cortex.fruvio.engine.merge.Levels
import studio.cortex.fruvio.screens.AchievementsScreen
import studio.cortex.fruvio.screens.BonusBoxScreen
import studio.cortex.fruvio.screens.HigherLowerScreen
import studio.cortex.fruvio.screens.InfoScreen
import studio.cortex.fruvio.screens.InfoTexts
import studio.cortex.fruvio.screens.LevelSelectScreen
import studio.cortex.fruvio.screens.MenuScreen
import studio.cortex.fruvio.screens.MergeGameScreen
import studio.cortex.fruvio.screens.MiniGamesScreen
import studio.cortex.fruvio.screens.PlinkoScreen
import studio.cortex.fruvio.screens.SettingsScreen
import studio.cortex.fruvio.screens.ShopScreen
import studio.cortex.fruvio.screens.SplashScreen

/** Optional capture mode: boot straight to a screen, render [frames] frames, save a PNG, exit. */
class Capture(
    val screen: String,
    val outPath: String,
    val frames: Int = 14,
)

private const val FADE_DURATION = 0.14f

/** Root application: shared rendering context + session state + screen navigation. */
class FruvioGame(private val capture: Capture? = null, private val haptics: Haptics = NoHaptics) : Game() {
    lateinit var batch: SpriteBatch
    lateinit var assets: Assets
    lateinit var audio: AudioManager
    lateinit var camera: OrthographicCamera
    lateinit var viewport: Viewport
    lateinit var progress: Progress

    var soundOn = true; private set
    var sfxVolume = 0.8f; private set
    var musicOn = true; private set
    var musicVolume = 0.6f; private set
    var vibrationOn = true; private set

    private lateinit var prefs: Preferences
    private var shotFrames = 0

    // ---- fade transition between screens ----
    private var fade = 0f
    private var fadeTarget = 0f
    private var pendingNav: (() -> Unit)? = null

    override fun create() {
        batch = SpriteBatch()
        camera = OrthographicCamera()
        viewport = FitViewport(Theme.W, Theme.H, camera)
        assets = Assets().also { it.load() }
        audio = AudioManager(this)
        prefs = Gdx.app.getPreferences("fruvio")
        progress = Progress(prefs)
        soundOn = prefs.getBoolean("soundOn", true)
        sfxVolume = prefs.getFloat("sfxVolume", 0.8f).coerceIn(0f, 1f)
        musicOn = prefs.getBoolean("musicOn", true)
        musicVolume = prefs.getFloat("musicVolume", 0.6f)
        vibrationOn = prefs.getBoolean("vibrationOn", true)
        if (capture == null) audio.updateMusic()

        if (capture != null) {
            navigateForCapture(capture)
            shotFrames = capture.frames
        } else {
            setScreen(SplashScreen(this))
        }
    }

    fun setSoundOn(v: Boolean) { soundOn = v; prefs.putBoolean("soundOn", v).flush() }
    fun setSfxVolume(v: Float, flush: Boolean = true) {
        sfxVolume = v.coerceIn(0f, 1f)
        prefs.putFloat("sfxVolume", sfxVolume)
        if (flush) prefs.flush()
    }
    fun setMusicOn(v: Boolean) { musicOn = v; prefs.putBoolean("musicOn", v).flush(); audio.updateMusic() }
    fun setMusicVolume(v: Float, flush: Boolean = true) {
        musicVolume = v.coerceIn(0f, 1f)
        prefs.putFloat("musicVolume", musicVolume)
        if (flush) prefs.flush()
        audio.updateMusic()
    }
    fun setVibrationOn(v: Boolean) { vibrationOn = v; prefs.putBoolean("vibrationOn", v).flush() }
    fun buzz(ms: Int = 25) { if (vibrationOn) haptics.vibrate(ms) }

    /**
     * Navigate with a short fade-out/fade-in. Falls back to an instant switch in capture
     * mode (screenshots must not contain a half-faded overlay) and for the very first screen.
     */
    private fun go(navigate: () -> Unit) {
        if (capture != null || screen == null) { navigate(); return }
        pendingNav = navigate
        fadeTarget = 1f
    }

    fun toSplash() = go { setScreen(SplashScreen(this)) }
    fun toMenu() = go { setScreen(MenuScreen(this)) }
    fun toLevelSelect() = go { setScreen(LevelSelectScreen(this)) }
    fun toMerge(level: LevelDef) = go { setScreen(MergeGameScreen(this, level)) }
    fun toSettings() = go { setScreen(SettingsScreen(this)) }
    fun toShop() = go { setScreen(ShopScreen(this)) }
    fun toAchievements() = go { setScreen(AchievementsScreen(this)) }
    fun toMiniGames() = go { setScreen(MiniGamesScreen(this)) }
    fun toPlinko() = go { setScreen(PlinkoScreen(this)) }
    fun toBonusBox() = go { setScreen(BonusBoxScreen(this)) }
    fun toHigherLower() = go { setScreen(HigherLowerScreen(this)) }
    fun toAbout() = go { setScreen(InfoScreen(this, "ABOUT", InfoTexts.ABOUT)) }
    fun toPrivacy() = go { setScreen(InfoScreen(this, "PRIVACY POLICY", InfoTexts.PRIVACY)) }
    fun toTerms() = go { setScreen(InfoScreen(this, "TERMS OF USE", InfoTexts.TERMS)) }

    override fun render() {
        super.render()
        renderFade()
        if (capture != null && shotFrames > 0) {
            shotFrames--
            if (shotFrames == 0) { grab(capture.outPath); Gdx.app.exit() }
        }
    }

    private fun renderFade() {
        val delta = Gdx.graphics.deltaTime
        val step = delta / FADE_DURATION
        fade = if (fade < fadeTarget) (fade + step).coerceAtMost(fadeTarget)
        else (fade - step).coerceAtLeast(fadeTarget)
        if (fade >= 1f) {
            pendingNav?.invoke(); pendingNav = null
            fadeTarget = 0f
        }
        if (fade > 0f) {
            viewport.apply()
            batch.projectionMatrix = camera.combined
            batch.begin()
            batch.setColor(0f, 0f, 0f, fade)
            batch.draw(assets.white, 0f, 0f, Theme.W, Theme.H)
            batch.setColor(Color.WHITE)
            batch.end()
        }
    }

    private fun navigateForCapture(capture: Capture) = when (capture.screen) {
        "menu" -> toMenu()
        "levelselect" -> toLevelSelect()
        "levelselect4" -> {
            toLevelSelect()
            (screen as LevelSelectScreen).debugShowWorld(4)
        }
        "merge" -> {
            toMerge(Levels.all.first())
            (screen as MergeGameScreen).debugSeedDemo()
        }
        "merge2" -> {
            toMerge(Levels.all[1])
            (screen as MergeGameScreen).debugSeedDemo()
        }
        "merge10" -> {
            toMerge(Levels.all[9])
            (screen as MergeGameScreen).debugSeedDemo()
        }
        "merge20" -> {
            toMerge(Levels.all[19])
            (screen as MergeGameScreen).debugSeedDemo()
        }
        "mergewin" -> {
            toMerge(Levels.all.first())
            (screen as MergeGameScreen).debugShowWinDialog()
        }
        "mergeloss" -> {
            toMerge(Levels.all.first())
            (screen as MergeGameScreen).debugShowLossDialog()
        }
        "mergehud0" -> {
            toMerge(Levels.all.first())
            (screen as MergeGameScreen).debugConfigureHud(0, false)
        }
        "mergehud120" -> {
            toMerge(Levels.all.first())
            (screen as MergeGameScreen).debugConfigureHud(120, false)
        }
        "mergehud9999" -> {
            toMerge(Levels.all.first())
            (screen as MergeGameScreen).debugConfigureHud(9999, true)
        }
        "combo1" -> { toMerge(Levels.all[19]); (screen as MergeGameScreen).debugSeedCombo(1) }
        "combo3" -> { toMerge(Levels.all[19]); (screen as MergeGameScreen).debugSeedCombo(3) }
        "combo6" -> { toMerge(Levels.all[19]); (screen as MergeGameScreen).debugSeedCombo(6) }
        "combo8" -> { toMerge(Levels.all[19]); (screen as MergeGameScreen).debugSeedCombo(8) }
        "settings" -> toSettings()
        "shop" -> toShop()
        "achievements" -> toAchievements()
        "minigames" -> toMiniGames()
        "plinko" -> toPlinko()
        "plinkowin" -> {
            toPlinko()
            (screen as PlinkoScreen).debugShowWinDialog()
        }
        "plinkoloss" -> {
            toPlinko()
            (screen as PlinkoScreen).debugShowLossDialog()
        }
        "bonusbox" -> toBonusBox()
        "bonuswin" -> {
            toBonusBox()
            (screen as BonusBoxScreen).debugShowWinDialog()
        }
        "bonusloss" -> {
            toBonusBox()
            (screen as BonusBoxScreen).debugShowLossDialog()
        }
        "higherlower" -> toHigherLower()
        "higherlowerwin" -> {
            toHigherLower()
            (screen as HigherLowerScreen).debugShowWinDialog()
        }
        "higherlowerloss" -> {
            toHigherLower()
            (screen as HigherLowerScreen).debugShowLossDialog()
        }
        "about" -> toAbout()
        "privacy" -> toPrivacy()
        "terms" -> toTerms()
        else -> toSplash()
    }

    private fun grab(path: String) {
        val pm = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        val flipped = Pixmap(pm.width, pm.height, Pixmap.Format.RGBA8888)
        for (y in 0 until pm.height) for (x in 0 until pm.width) {
            flipped.drawPixel(x, pm.height - 1 - y, pm.getPixel(x, y))
        }
        PixmapIO.writePNG(Gdx.files.absolute(path), flipped)
        pm.dispose(); flipped.dispose()
    }

    override fun dispose() {
        screen?.dispose()
        batch.dispose()
        assets.dispose()
        audio.dispose()
    }
}

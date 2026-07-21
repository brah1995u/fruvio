package studio.cortex.fruvio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.utils.Disposable
import studio.cortex.fruvio.engine.merge.FruitTier
import studio.cortex.fruvio.ui.UiSkin

/**
 * Loads and owns every runtime asset: the packed atlas (fruit symbols + UI chrome + icons), the
 * 3 full-screen backgrounds, and 2 FreeType fonts scaled to several sizes. No procedural
 * nine-patch/glow/vignette generation (unlike Flame Jester's own Assets.kt, which needed those
 * only because it had zero real UI art at first) — every UiSkin surface here is backed by a real
 * packed-atlas region. `ui_btn_rect_red` (as [UiSkin.dangerButton]) and `ui_orb_green` (as
 * [UiSkin.orbGreen]) back the Settings screen's toggle chrome — see [Controls].
 */
class Assets : Disposable {
    lateinit var uiSkin: UiSkin
    lateinit var atlas: TextureAtlas
    lateinit var bgWater: Texture
    lateinit var bgTropical: Texture
    lateinit var bgPanel: Texture
    lateinit var bgMenuCocktail: Texture
    lateinit var bgSplashFruvio: Texture
    /** Generated fruit-themed navigation controls, loaded outside the atlas. */
    lateinit var navHomeFruit: TextureRegion
    lateinit var navBackFruit: TextureRegion
    /** Ornate empty victory shell. Prize fruit, coins, copy and particles remain live-drawn. */
    lateinit var victoryFrame: TextureRegion

    // fonts
    lateinit var titleXL: BitmapFont
    lateinit var title: BitmapFont
    lateinit var victoryXL: BitmapFont
    lateinit var victoryTitle: BitmapFont
    /** [title]'s playful display face at button scale — compact HUD labels (booster row) that
     *  should read as "fruity" rather than the neutral [buttonLight] used by normal buttons. */
    lateinit var titleSmall: BitmapFont
    lateinit var buttonLight: BitmapFont
    lateinit var bodyLight: BitmapFont
    lateinit var captionLight: BitmapFont
    lateinit var small: BitmapFont
    lateinit var num: BitmapFont

    /** 4x4 solid white texture — fade-transition overlay and tinted-rect fills (progress bar,
     *  slider track), not a "procedural UI" asset. */
    lateinit var white: Texture

    lateinit var iconArrow: TextureRegion
    /** [iconArrow] flipped horizontally — a left-pointing back arrow, not a separate asset. */
    lateinit var iconBack: TextureRegion
    lateinit var iconStar: TextureRegion
    lateinit var iconCart: TextureRegion
    lateinit var iconPlay: TextureRegion
    lateinit var iconMiniGames: TextureRegion
    lateinit var iconNavCart: TextureRegion
    lateinit var iconNavStar: TextureRegion
    lateinit var iconSettingsFruit: TextureRegion
    lateinit var iconHome: TextureRegion
    lateinit var iconMenu: TextureRegion
    lateinit var iconMute: TextureRegion
    lateinit var iconClose: TextureRegion
    lateinit var iconSound: TextureRegion

    private val disposables = ArrayList<Disposable>()
    private val symRegions = HashMap<FruitTier, TextureRegion>()

    fun load() {
        atlas = TextureAtlas(Gdx.files.internal("atlas/fruvio.atlas")).also { loaded ->
            // The atlas was exported with Nearest filtering, which makes every Figma outline
            // visibly stair-step when a phone scale is not an exact integer. Linear filtering is
            // applied once to its single page, so fruits, buttons, frames and icons stay clean.
            loaded.textures.forEach { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
            disposables.add(loaded)
        }
        bgWater = tex("backgrounds/bg_water.png")
        bgTropical = tex("backgrounds/bg_tropical.png")
        bgPanel = tex("backgrounds/bg_panel.png")
        bgMenuCocktail = tex("backgrounds/bg_menu_fruvio.png")
        bgSplashFruvio = tex("backgrounds/bg_splash_fruvio.png")
        navHomeFruit = TextureRegion(tex("backgrounds/nav_home_fruit.png"))
        navBackFruit = TextureRegion(tex("backgrounds/nav_back_fruit.png"))
        victoryFrame = TextureRegion(tex("backgrounds/victory_frame_v3.png"))

        FruitTier.entries.forEach { t -> symRegions[t] = atlasRegion("sym_${t.name.lowercase()}") }

        iconArrow = atlasRegion("icon_arrow")
        iconBack = TextureRegion(iconArrow).apply { flip(true, false) }
        iconStar = atlasRegion("icon_star")
        iconCart = atlasRegion("icon_cart")
        iconPlay = atlasRegion("icon_play")
        iconMiniGames = atlasRegion("icon_minigames")
        iconNavCart = atlasRegion("icon_nav_cart")
        iconNavStar = atlasRegion("icon_nav_star")
        iconSettingsFruit = atlasRegion("icon_settings_fruit")
        iconHome = atlasRegion("icon_home")
        iconMenu = atlasRegion("icon_menu")
        iconMute = atlasRegion("icon_mute")
        iconClose = atlasRegion("icon_close")
        iconSound = atlasRegion("icon_sound")

        loadFonts()
        white = solid(Color.WHITE)
        loadUiSkin()
    }

    fun region(tier: FruitTier): TextureRegion = symRegions.getValue(tier)

    /** Alternate the two fruit-scene backgrounds; bgPanel is reserved for Settings. */
    fun fruitBackground(seed: Int): Texture = if (seed % 2 == 0) bgTropical else bgWater

    private fun atlasRegion(name: String): TextureRegion =
        requireNotNull(atlas.findRegion(name)) { "Missing atlas region $name" }

    private fun tex(path: String): Texture =
        Texture(Gdx.files.internal(path), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
            disposables.add(this)
        }

    private fun loadFonts() {
        val display = FreeTypeFontGenerator(Gdx.files.internal("fonts/TitanOne.ttf"))
        val ui = FreeTypeFontGenerator(Gdx.files.internal("fonts/Rowdies-Bold.ttf"))
        disposables.add(display); disposables.add(ui)

        fun make(gen: FreeTypeFontGenerator, sz: Int, border: Float): BitmapFont {
            val p = FreeTypeFontParameter().apply {
                size = sz
                color = Color.WHITE
                borderWidth = border
                borderColor = Theme.panelDeep
                shadowOffsetX = 0; shadowOffsetY = 3
                shadowColor = Color(0f, 0f, 0f, 0.45f)
                minFilter = Texture.TextureFilter.Linear
                magFilter = Texture.TextureFilter.Linear
            }
            return gen.generateFont(p).also { disposables.add(it) }
        }

        fun makeVictory(sz: Int, border: Float): BitmapFont {
            val p = FreeTypeFontParameter().apply {
                size = sz
                color = Color.valueOf("FFF8D8")
                borderWidth = border
                borderColor = Color.valueOf("6F2A10")
                shadowOffsetX = 0; shadowOffsetY = 5
                shadowColor = Color(0.12f, 0.03f, 0.02f, 0.75f)
                minFilter = Texture.TextureFilter.Linear
                magFilter = Texture.TextureFilter.Linear
            }
            return display.generateFont(p).also { disposables.add(it) }
        }
        titleXL = make(display, 150, 6f)
        title = make(display, 86, 4f)
        victoryXL = makeVictory(150, 9f)
        victoryTitle = makeVictory(86, 7f)
        titleSmall = make(display, 34, 2f)
        num = make(display, 60, 4f)
        buttonLight = make(ui, 44, 2.5f)
        bodyLight = make(ui, 32, 2.5f)
        captionLight = make(ui, 24, 2f)
        small = make(ui, 25, 2f)
    }

    private fun loadUiSkin() {
        // Insets re-measured 2026-07-22 by zoomed corner inspection (not a flat-color scan, which
        // misreads these gradient-filled buttons' interior as "still inside the corner") — the
        // first pass under-measured the horizontal inset on both buttons, letting part of the
        // rounded corner fall inside the stretchable middle and visibly warp on resize.
        val primaryButton = NinePatch(atlasRegion("ui_btn_rect_blue"), 60, 60, 60, 60)
        val secondaryButtonRegion = atlasRegion("ui_btn_square_blue")
        // ui_panel_square_gold is the Figma surface authored for large structural areas. Its
        // navy interior keeps every supplied fruit legible; the red asset remains a short action
        // surface and is never stretched into a game board.
        val panel = NinePatch(atlasRegion("ui_btn_rect_red"), 60, 60, 60, 60)
        val jarPanel = NinePatch(atlasRegion("ui_panel_square_gold"), 34, 34, 35, 35)
        val coinBadge = atlasRegion("ui_badge_circle_gold")
        // Plain scaled region, not nine-patched: at toggle scale (168x88) this asset's corner
        // radius exceeds the target height, so patch-stretching pinches the corners into a lens
        // shape instead of a clean pill — see UiSkin's own doc on this field.
        val dangerButton = atlasRegion("ui_btn_rect_red")
        val orbGreen = atlasRegion("ui_orb_green")
        uiSkin = UiSkin(primaryButton, secondaryButtonRegion, panel, jarPanel, coinBadge, dangerButton, orbGreen)
    }

    private fun solid(c: Color): Texture {
        val p = Pixmap(4, 4, Pixmap.Format.RGBA8888); p.setColor(c); p.fill()
        return Texture(p).also { p.dispose(); disposables.add(it) }
    }

    override fun dispose() {
        disposables.forEach { it.dispose() }
        disposables.clear()
    }
}

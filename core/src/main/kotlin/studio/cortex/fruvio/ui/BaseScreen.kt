package studio.cortex.fruvio.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import studio.cortex.fruvio.FruvioGame
import studio.cortex.fruvio.Sfx
import studio.cortex.fruvio.Theme

/**
 * Base for every screen: owns a button list, translates touch to virtual coordinates,
 * and runs the standard update→clear→batch render loop against the shared viewport.
 * Also owns the optional widget lists (toggles/sliders/scroll areas) and a modal
 * [Dialog] slot — while a dialog is open it captures all input.
 */
abstract class BaseScreen(val game: FruvioGame) : ScreenAdapter(), InputProcessor {
    protected val batch: SpriteBatch get() = game.batch
    protected val a get() = game.assets
    protected val buttons = ArrayList<Button>()
    protected val toggles = ArrayList<Toggle>()
    protected val sliders = ArrayList<Slider>()
    protected val scrolls = ArrayList<ScrollArea>()
    private val tmp = Vector3()

    protected var dialog: Dialog? = null
        private set

    protected fun add(b: Button): Button { buttons.add(b); return b }
    protected fun add(t: Toggle): Toggle { toggles.add(t); return t }
    protected fun add(s: Slider): Slider { sliders.add(s); return s }
    protected fun add(s: ScrollArea): ScrollArea { scrolls.add(s); return s }

    /** Consistent navigation contract: Back is always left; Home is always right. */
    protected fun addBackNavigation(onClick: () -> Unit): Button = add(Button(
        48f, 2244f, 120f, 120f, style = Button.Style.NONE, region = game.assets.navBackFruit,
        role = ActionRole.BACK, onClick = onClick,
    ))

    protected fun addHomeNavigation(): Button = add(Button(
        Theme.W - 168f, 2244f, 120f, 120f, style = Button.Style.NONE,
        region = game.assets.navHomeFruit, role = ActionRole.HOME, onClick = { game.toMenu() },
    ))

    protected fun drawNavigation(back: Button, home: Button) {
        Ui.button(batch, game.assets, back)
        Ui.button(batch, game.assets, home)
    }

    protected fun openDialog(d: Dialog) { dialog = d; d.show() }
    protected fun closeDialog() { dialog?.hide(); dialog = null }

    override fun show() {
        Gdx.input.inputProcessor = this
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
    }

    override fun render(delta: Float) {
        for (b in buttons) Ui.tick(b, delta)
        for (t in toggles) Controls.tick(t, delta)
        for (s in sliders) Controls.tick(s, delta)
        for (s in scrolls) s.tick(delta)
        dialog?.tick(delta)
        update(delta)
        ScreenUtils.clear(0.06f, 0.03f, 0.10f, 1f)
        game.viewport.apply()
        batch.projectionMatrix = game.camera.combined
        batch.begin()
        draw(delta)
        dialog?.draw(batch, a)
        batch.end()
    }

    protected abstract fun draw(delta: Float)
    protected open fun update(delta: Float) {}

    override fun resize(width: Int, height: Int) {
        game.viewport.update(width, height, true)
    }

    // ---- input → virtual coords ----
    private fun toVirtual(sx: Int, sy: Int): Vector3 {
        tmp.set(sx.toFloat(), sy.toFloat(), 0f)
        game.viewport.unproject(tmp)
        return tmp
    }

    override fun touchDown(sx: Int, sy: Int, pointer: Int, button: Int): Boolean {
        val v = toVirtual(sx, sy)
        dialog?.let { d ->
            if (d.visible) {
                for (b in d.buttons) if (b.enabled && b.contains(v.x, v.y)) { b.pressed = true; b.releaseAnim = 0f }
                return true
            }
        }
        for (s in sliders) if (s.contains(v.x, v.y)) { s.dragging = true; s.setFrom(v.x); return true }
        for (s in scrolls) if (s.contains(v.x, v.y)) { s.touchDown(v.y); break }
        for (b in buttons) if (b.enabled && b.inputEnabled && b.contains(v.x, v.y)) { b.pressed = true; b.releaseAnim = 0f; return true }
        for (t in toggles) if (t.contains(v.x, v.y)) return true
        return onTouchDown(v.x, v.y)
    }

    override fun touchDragged(sx: Int, sy: Int, pointer: Int): Boolean {
        val v = toVirtual(sx, sy)
        if (dialog?.visible == true) return true
        var handled = false
        for (s in sliders) if (s.dragging) { s.setFrom(v.x); handled = true }
        for (s in scrolls) if (s.touchDragged(v.y)) {
            handled = true
            // a real scroll cancels any pending button press inside the list
            for (b in buttons) b.pressed = false
        }
        if (!handled) handled = onTouchDragged(v.x, v.y)
        return handled
    }

    override fun touchUp(sx: Int, sy: Int, pointer: Int, button: Int): Boolean {
        val v = toVirtual(sx, sy)
        dialog?.let { d ->
            if (d.visible) {
                for (b in d.buttons) {
                    if (b.pressed) {
                        if (b.enabled && b.contains(v.x, v.y)) { game.audio.play(Sfx.CLICK); game.buzz(); b.onClick() }
                        b.pressed = false
                    }
                }
                return true
            }
        }
        var handled = false
        for (s in sliders) if (s.dragging) { s.dragging = false; s.onRelease(); handled = true }
        var scrolled = false
        for (s in scrolls) if (s.touchUp()) scrolled = true
        if (scrolled) { for (b in buttons) b.pressed = false; return true }
        for (b in buttons) {
            if (b.pressed) {
                if (b.enabled && b.inputEnabled && b.contains(v.x, v.y)) { game.audio.play(Sfx.CLICK); game.buzz(); b.onClick(); handled = true }
                b.pressed = false
            }
        }
        if (!handled) for (t in toggles) if (t.contains(v.x, v.y)) {
            game.audio.play(Sfx.CLICK); game.buzz(); t.toggle(); handled = true
        }
        if (!handled) handled = onTouchUp(v.x, v.y)
        return handled
    }

    protected open fun onTouchDown(x: Float, y: Float): Boolean = false
    protected open fun onTouchUp(x: Float, y: Float): Boolean = false
    protected open fun onTouchDragged(x: Float, y: Float): Boolean = false

    override fun keyDown(keycode: Int): Boolean {
        if (keycode != Input.Keys.BACK) return false
        if (dialog?.visible == true) {
            dialog?.dismiss()
            return true
        }
        val action = buttons.firstOrNull {
            it.enabled && it.inputEnabled && it.role == ActionRole.CLOSE
        } ?: buttons.firstOrNull {
            it.enabled && it.inputEnabled && it.role == ActionRole.BACK
        } ?: buttons.firstOrNull {
            it.enabled && it.inputEnabled && it.role == ActionRole.PAUSE
        } ?: return false
        game.audio.play(Sfx.CLICK)
        action.onClick()
        return true
    }
    override fun keyUp(keycode: Int): Boolean = false
    override fun keyTyped(character: Char): Boolean = false
    override fun mouseMoved(sx: Int, sy: Int): Boolean = false
    override fun scrolled(x: Float, y: Float): Boolean = false
    override fun touchCancelled(sx: Int, sy: Int, pointer: Int, button: Int): Boolean = false
}

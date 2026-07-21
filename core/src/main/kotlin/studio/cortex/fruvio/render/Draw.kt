package studio.cortex.fruvio.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import studio.cortex.fruvio.Theme

/** Shared immediate-mode drawing helpers in the 1080x2400 virtual space. Forked from Flame
 *  Jester's Draw.kt as-is, minus `wheelPointer` (depended on Flame Jester's slot-specific
 *  `WheelFace` engine type, which doesn't exist here). */
object Draw {
    val layout = GlyphLayout()

    /** Draw a texture covering the whole virtual screen (scale-to-fill, centre-crop). */
    fun cover(batch: SpriteBatch, tex: Texture, alpha: Float = 1f) {
        val scale = maxOf(Theme.W / tex.width, Theme.H / tex.height)
        val w = tex.width * scale; val h = tex.height * scale
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(tex, (Theme.W - w) / 2f, (Theme.H - h) / 2f, w, h)
        batch.setColor(Color.WHITE)
    }

    /** Draw a region scaled to fit a box, centred at (cx,cy), aspect preserved. */
    fun imageFit(
        batch: SpriteBatch, region: TextureRegion, cx: Float, cy: Float,
        boxW: Float, boxH: Float, scale: Float = 1f, alpha: Float = 1f,
    ) {
        val s = minOf(boxW / region.regionWidth, boxH / region.regionHeight) * scale
        val w = region.regionWidth * s; val h = region.regionHeight * s
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(region, cx - w / 2f, cy - h / 2f, w, h)
        batch.setColor(Color.WHITE)
    }

    /** Tint-aware variant for monochrome HUD icons. It restores batch colour after drawing. */
    fun imageFitTinted(
        batch: SpriteBatch, region: TextureRegion, cx: Float, cy: Float,
        boxW: Float, boxH: Float, color: Color, scale: Float = 1f, alpha: Float = 1f,
    ) {
        val s = minOf(boxW / region.regionWidth, boxH / region.regionHeight) * scale
        val w = region.regionWidth * s; val h = region.regionHeight * s
        batch.setColor(color.r, color.g, color.b, color.a * alpha)
        batch.draw(region, cx - w / 2f, cy - h / 2f, w, h)
        batch.setColor(Color.WHITE)
    }

    /** Rotation overload for Texture that does not allocate a TextureRegion per frame. */
    fun imageFitRotated(
        batch: SpriteBatch, tex: Texture, cx: Float, cy: Float,
        boxW: Float, boxH: Float, degrees: Float, scale: Float = 1f,
    ) {
        val s = minOf(boxW / tex.width, boxH / tex.height) * scale
        val w = tex.width * s; val h = tex.height * s
        batch.draw(
            tex, cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h,
            1f, 1f, degrees, 0, 0, tex.width, tex.height, false, false,
        )
    }

    /** Rotation overload for TextureRegion (packed-atlas sub-regions) — reads the region's own
     *  width/height so a body-driven sprite (e.g. a falling fruit) renders its correct atlas
     *  sub-image, not the whole atlas page the [Texture] overload above would draw. */
    fun imageFitRotated(
        batch: SpriteBatch, region: TextureRegion, cx: Float, cy: Float,
        boxW: Float, boxH: Float, degrees: Float, scale: Float = 1f,
    ) {
        val s = minOf(boxW / region.regionWidth, boxH / region.regionHeight) * scale
        val w = region.regionWidth * s; val h = region.regionHeight * s
        batch.draw(
            region, cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h,
            1f, 1f, degrees,
        )
    }

    /** Texture overload that draws directly and performs no render-loop allocation. */
    fun imageFit(
        batch: SpriteBatch, tex: Texture, cx: Float, cy: Float,
        boxW: Float, boxH: Float, scale: Float = 1f, alpha: Float = 1f,
    ) {
        val s = minOf(boxW / tex.width, boxH / tex.height) * scale
        val w = tex.width * s; val h = tex.height * s
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(tex, cx - w / 2f, cy - h / 2f, w, h)
        batch.setColor(Color.WHITE)
    }

    /** Solid rotated rectangle from a flat texture (e.g. [studio.cortex.fruvio.Assets.white]).
     *  Unlike [imageFitRotated] this deliberately does NOT preserve the source aspect ratio, so it
     *  can draw long thin shapes — light rays, confetti strips — that a square 4x4 source could
     *  never produce through the aspect-preserving fit helpers. Restores batch colour after. */
    fun rectRotated(
        batch: SpriteBatch, tex: Texture, cx: Float, cy: Float, w: Float, h: Float, degrees: Float,
        color: Color, alpha: Float = 1f,
    ) {
        batch.setColor(color.r, color.g, color.b, color.a * alpha)
        batch.draw(
            tex, cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h,
            1f, 1f, degrees, 0, 0, tex.width, tex.height, false, false,
        )
        batch.setColor(Color.WHITE)
    }

    fun glow(batch: SpriteBatch, tex: Texture, cx: Float, cy: Float, size: Float, color: Color, alpha: Float = 1f) {
        batch.setColor(color.r, color.g, color.b, alpha)
        batch.draw(tex, cx - size / 2f, cy - size / 2f, size, size)
        batch.setColor(Color.WHITE)
    }

    fun panel(batch: SpriteBatch, np: NinePatch, x: Float, y: Float, w: Float, h: Float, color: Color = Color.WHITE) {
        np.color = color
        np.draw(batch, x, y, w, h)
        np.color = Color.WHITE
    }

    fun text(batch: SpriteBatch, font: BitmapFont, str: String, x: Float, y: Float, color: Color) {
        font.color = color
        font.draw(batch, str, x, y)
    }

    /** Centre text horizontally on cx; y is the baseline-top (cap) centre line. */
    fun textCentered(batch: SpriteBatch, font: BitmapFont, str: String, cx: Float, cy: Float, color: Color) {
        layout.setText(font, str)
        font.color = color
        font.draw(batch, str, cx - layout.width / 2f, cy + layout.height / 2f)
    }

    fun textCenteredMultiline(
        batch: SpriteBatch, font: BitmapFont, str: String,
        cx: Float, cy: Float, width: Float, color: Color,
    ) {
        layout.setText(font, str, color, width, com.badlogic.gdx.utils.Align.center, false)
        font.color = color
        font.draw(batch, layout, cx - width / 2f, cy + layout.height / 2f + 12f)
    }

    fun width(font: BitmapFont, str: String): Float {
        layout.setText(font, str); return layout.width
    }

    // ---- scissor clipping (virtual-space rect) ----
    private val clipRect = Rectangle()
    private val clipScissors = Rectangle()

    /** Begin clipping to a virtual-space rect. Returns false (and clips nothing) if the rect is off-screen. */
    fun clipBegin(batch: SpriteBatch, camera: Camera, x: Float, y: Float, w: Float, h: Float): Boolean {
        batch.flush()
        clipRect.set(x, y, w, h)
        ScissorStack.calculateScissors(camera, batch.transformMatrix, clipRect, clipScissors)
        return ScissorStack.pushScissors(clipScissors)
    }

    fun clipEnd(batch: SpriteBatch) {
        batch.flush()
        ScissorStack.popScissors()
    }
}

package com.android.voidrise.game.ui

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils

/** Short, bold, blinking center-screen alerts. */
class FlightBannerHud {

    enum class Kind { ATMOSPHERE, WARP_BLOCKED }

    private val layout = GlyphLayout()

    private var kind: Kind? = null
    private var timer  = 0f
    private var pulse  = 0f
    var alpha          = 0f
        private set

    fun showAtmosphereEntry() = show(Kind.ATMOSPHERE)

    fun showWarpBlocked() = show(Kind.WARP_BLOCKED)

    fun show(kind: Kind) {
        this.kind = kind
        timer = DISPLAY_SEC
        alpha = 0f
        pulse = 0f
    }

    fun update(dt: Float) {
        pulse += dt
        if (kind == null) return
        timer -= dt
        val fadeIn  = ((DISPLAY_SEC - timer) / FADE_IN_SEC).coerceIn(0f, 1f)
        val fadeOut = (timer / FADE_OUT_SEC).coerceIn(0f, 1f)
        alpha = fadeIn * fadeOut
        if (timer <= 0f) {
            kind = null
            alpha = 0f
        }
    }

    /** Blinking screen bars — no panel box. */
    fun drawBackground(shapes: ShapeRenderer, sw: Float, sh: Float) {
        if (kind == null || alpha < 0.03f) return

        val c = content(kind!!)
        val blink = if ((pulse * BLINK_HZ).toInt() % 2 == 0) 1f else 0.22f
        val a = alpha * blink
        val barH = 14f
        val midY = sh * 0.54f

        shapes.color.set(c.r, c.g, c.b, 0.55f * a)
        shapes.rect(0f, midY + 28f, sw, barH)
        shapes.rect(0f, midY - 28f - barH, sw, barH)

        shapes.color.set(c.r, c.g, c.b, 0.12f * a)
        shapes.rect(0f, 0f, sw, 6f)
        shapes.rect(0f, sh - 6f, sw, 6f)
    }

    fun drawText(batch: SpriteBatch, font: BitmapFont, sw: Float, sh: Float) {
        if (kind == null || alpha < 0.03f) return

        val c = content(kind!!)
        val blink = if ((pulse * BLINK_HZ).toInt() % 2 == 0) 1f else 0.30f
        val a = alpha * blink
        val scale = TEXT_SCALE
        val midY = sh * 0.54f

        font.data.setScale(scale)
        layout.setText(font, c.text)

        val x = (sw - layout.width) * 0.5f
        val y = midY + layout.height * 0.5f

        font.color.set(0f, 0f, 0f, a * 0.65f)
        font.draw(batch, layout, x + 3f, y - 3f)

        font.color.set(c.r, c.g, c.b, a)
        font.draw(batch, layout, x, y)

        font.data.setScale(1f)
    }

    private fun content(kind: Kind) = when (kind) {
        Kind.ATMOSPHERE -> FlashContent("ATMOSPHÄREN EINTRITT", 0.20f, 0.95f, 1.0f)
        Kind.WARP_BLOCKED -> FlashContent("WARP GESPERRT", 1.0f, 0.28f, 0.10f)
    }

    private data class FlashContent(val text: String, val r: Float, val g: Float, val b: Float)

    companion object {
        private const val DISPLAY_SEC  = 2.6f
        private const val FADE_IN_SEC  = 0.12f
        private const val FADE_OUT_SEC = 0.55f
        private const val BLINK_HZ     = 5.5f
        private const val TEXT_SCALE   = 2.85f
    }
}

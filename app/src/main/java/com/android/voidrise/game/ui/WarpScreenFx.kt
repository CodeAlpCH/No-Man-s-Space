package com.android.voidrise.game.ui

import com.android.voidrise.game.warp.WarpSystem
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/** Warp status messages only — no screen effects. */
object WarpScreenFx {

    private val layout = GlyphLayout()

    fun drawOverlayText(
        batch: SpriteBatch,
        messageFont: BitmapFont,
        sw: Float,
        sh: Float,
        warp: WarpSystem,
    ) {
        if (warp.hudMessage != null && warp.messageAlpha > 0.05f) {
            drawMessage(batch, messageFont, sw, sh, warp.hudMessage!!, warp.messageAlpha)
        }
    }

    private fun drawMessage(batch: SpriteBatch, font: BitmapFont, sw: Float, sh: Float, text: String, alpha: Float) {
        layout.setText(font, text)
        font.color.set(1f, 0.45f, 0.25f, alpha * 0.95f)
        font.draw(batch, layout, sw * 0.5f - layout.width * 0.5f, sh * 0.72f)
    }
}

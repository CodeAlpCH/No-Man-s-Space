package com.android.voidrise.game.ui

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils

/** Minimal speed readout — plain text, no panel. */
object SpeedHud {

    private val layout = GlyphLayout()

    private var textX = 0f
    private var textY = 0f

    fun layout(screenW: Int, screenH: Int, leverCenterX: Float, leverBottomY: Float) {
        textX = leverCenterX + screenW * 0.11f
        textY = leverBottomY + screenH * 0.055f
    }

    fun drawText(
        batch: SpriteBatch,
        font: BitmapFont,
        speedKmS: Float,
        warpActive: Boolean,
    ) {
        val text = "${formatValue(speedKmS)} km/s"
        layout.setText(font, text)

        if (warpActive) {
            font.color.set(0.78f, 0.62f, 1f, 0.95f)
        } else {
            val t = (speedKmS / 120f).coerceIn(0f, 1f)
            font.color.set(
                MathUtils.lerp(0.62f, 0.92f, t),
                MathUtils.lerp(0.78f, 0.98f, t),
                1f,
                MathUtils.lerp(0.62f, 0.88f, t),
            )
        }

        font.draw(batch, layout, textX, textY)
    }

    private fun formatValue(kmS: Float): String = when {
        kmS >= 1000f -> String.format("%,d", kmS.toInt()).replace(',', ' ')
        kmS >= 100f  -> kmS.toInt().toString()
        kmS >= 1f    -> String.format("%.1f", kmS)
        else         -> "0"
    }
}

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

    fun layout(screenW: Int, screenH: Int, @Suppress("UNUSED_PARAMETER") leverCenterX: Float, leverBottomY: Float) {
        // Own lane: left-center, well above throttle foot and away from warp button
        textX = screenW * 0.14f
        textY = (screenH * 0.13f).coerceAtLeast(leverBottomY + screenH * 0.04f)
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
            font.color.set(0.85f, 0.72f, 1f, 0.98f)
        } else {
            val t = (speedKmS / 120f).coerceIn(0f, 1f)
            font.color.set(
                MathUtils.lerp(0.78f, 0.98f, t),
                MathUtils.lerp(0.88f, 1f, t),
                1f,
                MathUtils.lerp(0.82f, 0.96f, t),
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

package com.android.voidrise.game.ui

import com.android.voidrise.game.entities.Spaceship
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils

/**
 * Speed instrument beside the throttle — panel + large readout, clear of notch cameras.
 */
object SpeedHud {

    private val layout = GlyphLayout()
    private val layoutSmall = GlyphLayout()

    private var panelLeft = 0f
    private var panelBot  = 0f
    private var panelW    = 0f
    private var panelH    = 0f
    private var barLeft   = 0f
    private var barY      = 0f
    private var barW      = 0f
    private var valueX    = 0f
    private var valueY    = 0f

    fun layout(screenW: Int, screenH: Int, leverCenterX: Float, leverBottomY: Float) {
        panelLeft = leverCenterX + screenW * 0.085f
        panelW    = screenW * 0.26f
        panelH    = (screenH * 0.11f).coerceIn(78f, 110f)
        panelBot  = leverBottomY.coerceAtLeast(screenH * 0.042f)

        val pad = panelW * 0.09f
        barLeft = panelLeft + pad
        barW    = panelW - pad * 2f
        barY    = panelBot + panelH * 0.20f
        valueX  = panelLeft + pad
        valueY  = panelBot + panelH * 0.52f
    }

    fun drawBackground(shapes: ShapeRenderer) {
        shapes.color.set(0.05f, 0.07f, 0.14f, 0.78f)
        shapes.rect(panelLeft, panelBot, panelW, panelH)
        shapes.color.set(0.30f, 0.58f, 1f, 0.22f)
        shapes.rect(panelLeft, panelBot + panelH - 2f, panelW, 2f)
    }

    fun drawBar(shapes: ShapeRenderer, speedKmS: Float, warpActive: Boolean) {
        val maxV = if (warpActive) Spaceship.WARP_SPEED else Spaceship.MAX_CRUISE_SPEED
        val fill = (speedKmS / maxV).coerceIn(0f, 1f)

        shapes.color.set(1f, 1f, 1f, 0.08f)
        shapes.rect(barLeft, barY, barW, 5f)

        val cr = if (warpActive) 0.70f else MathUtils.lerp(0.30f, 1.0f, fill)
        val cg = if (warpActive) 0.50f else MathUtils.lerp(0.80f, 0.35f, fill)
        val cb = if (warpActive) 1.0f else 1f - fill * 0.4f
        shapes.color.set(cr, cg, cb, 0.88f)
        shapes.rect(barLeft, barY, barW * fill, 5f)
    }

    fun drawText(
        batch: SpriteBatch,
        labelFont: BitmapFont,
        valueFont: BitmapFont,
        speedKmS: Float,
        warpActive: Boolean,
    ) {
        labelFont.color.set(0.55f, 0.78f, 1f, 0.70f)
        layoutSmall.setText(labelFont, "GESCHW.")
        labelFont.draw(batch, layoutSmall, valueX, panelBot + panelH - 10f)

        val value = formatValue(speedKmS)
        valueFont.color.set(0.92f, 0.97f, 1f, if (warpActive) 1f else 0.90f)
        layout.setText(valueFont, value)
        valueFont.draw(batch, layout, valueX, valueY + layout.height * 0.35f)

        labelFont.color.set(0.65f, 0.85f, 1f, 0.75f)
        layoutSmall.setText(labelFont, "km/s")
        labelFont.draw(batch, layoutSmall, valueX + layout.width + 6f, valueY)
    }

    private fun formatValue(kmS: Float): String = when {
        kmS >= 1000f -> String.format("%,d", kmS.toInt()).replace(',', ' ')
        kmS >= 100f  -> kmS.toInt().toString()
        kmS >= 1f    -> String.format("%.1f", kmS)
        else         -> "0"
    }
}

package com.android.voidrise.game.ui

import com.android.voidrise.game.warp.WarpSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.min

class WarpButton {

    var cx = 0f
    var cy = 0f
    val radius = 52f

    private val layout = GlyphLayout()
    private val layoutSmall = GlyphLayout()
    private var wasTouched = false

    fun layout(screenW: Int, screenH: Int) {
        cx = screenW * 0.50f
        cy = radius + 42f
    }

    fun update(warp: WarpSystem, shipPos: com.badlogic.gdx.math.Vector3) {
        var touched = false
        for (i in 0 until min(Gdx.input.maxPointers, 5)) {
            if (!Gdx.input.isTouched(i)) continue
            val tx = Gdx.input.getX(i).toFloat()
            val ty = Gdx.graphics.height - Gdx.input.getY(i).toFloat()
            if (Vector2(tx - cx, ty - cy).len() <= radius * 1.5f) {
                touched = true
                break
            }
        }
        if (touched && !wasTouched) {
            if (warp.isEngaged()) warp.cancel()
            else warp.requestActivation(shipPos)
        }
        wasTouched = touched
    }

    fun drawFilled(shapes: ShapeRenderer, warp: WarpSystem, time: Float) {
        val idle      = warp.state == WarpSystem.State.IDLE
        val countdown = warp.state == WarpSystem.State.COUNTDOWN
        val active    = warp.state == WarpSystem.State.ACTIVE
        val progress  = warp.chargeProgress()

        if (countdown || active) {
            val pulse = MathUtils.sin(time * (if (active) 5.5f else 3.2f)) * 0.5f + 0.5f
            val glowA = if (active) 0.10f + pulse * 0.08f else 0.06f + progress * 0.10f
            shapes.color.set(
                if (active) 0.62f else 0.28f,
                if (active) 0.42f else 0.82f,
                1f,
                glowA,
            )
            shapes.circle(cx, cy, radius * 1.38f)
        }

        // Inner disc — very subtle, not a solid 1990s blob
        shapes.color.set(0.04f, 0.07f, 0.14f, if (idle) 0.22f else 0.38f)
        shapes.circle(cx, cy, radius * 0.72f)

        if (countdown) {
            // Filled sweep shows elapsed charge time (0 → full over 3 s)
            shapes.color.set(0.35f, 0.88f, 1f, 0.28f)
            shapes.arc(cx, cy, radius * 0.88f, 90f, -360f * progress)
        }

        if (active) {
            shapes.color.set(0.58f, 0.38f, 1f, 0.18f + MathUtils.sin(time * 4f) * 0.06f)
            shapes.circle(cx, cy, radius * 0.55f)
        }
    }

    fun drawLines(shapes: ShapeRenderer, warp: WarpSystem, time: Float) {
        val idle      = warp.state == WarpSystem.State.IDLE
        val countdown = warp.state == WarpSystem.State.COUNTDOWN
        val active    = warp.state == WarpSystem.State.ACTIVE
        val progress  = warp.chargeProgress()
        val r         = radius

        // Outer track
        shapes.color.set(0.42f, 0.72f, 1f, if (idle) 0.28f else 0.45f)
        shapes.circle(cx, cy, r)

        // Inner guide ring
        shapes.color.set(1f, 1f, 1f, 0.07f)
        shapes.circle(cx, cy, r * 0.72f)

        when {
            countdown -> {
                // Bright arc = remaining time (shrinks 360° → 0° over 3 s)
                val remain = 1f - progress
                if (remain > 0.01f) {
                    shapes.color.set(0.48f, 0.94f, 1f, 0.92f)
                    shapes.arc(cx, cy, r, 90f, -360f * remain)
                }
                // Tick marks at 0 / 1 / 2 / 3 s
                for (i in 0..3) {
                    val a = MathUtils.degreesToRadians * (90f - i * 90f)
                    val cos = MathUtils.cos(a)
                    val sin = MathUtils.sin(a)
                    shapes.color.set(0.55f, 0.90f, 1f, 0.55f)
                    shapes.rectLine(
                        cx + cos * r * 0.82f, cy + sin * r * 0.82f,
                        cx + cos * r * 0.96f, cy + sin * r * 0.96f,
                        1.8f,
                    )
                }
            }
            active -> {
                val pulse = MathUtils.sin(time * 5f) * 0.5f + 0.5f
                shapes.color.set(0.72f, 0.52f, 1f, 0.55f + pulse * 0.35f)
                shapes.circle(cx, cy, r * 0.88f)
            }
            else -> {
                // Corner dots — minimal sci-fi hint
                shapes.color.set(0.50f, 0.85f, 1f, 0.35f)
                for (i in 0 until 4) {
                    val a = MathUtils.degreesToRadians * (45f + i * 90f)
                    val px = cx + MathUtils.cos(a) * r * 0.62f
                    val py = cy + MathUtils.sin(a) * r * 0.62f
                    shapes.circle(px, py, 2.2f)
                }
            }
        }
    }

    fun drawLabel(batch: SpriteBatch, font: BitmapFont, smallFont: BitmapFont, warp: WarpSystem) {
        when (warp.state) {
            WarpSystem.State.COUNTDOWN -> {
                val digit = warp.countdownDigit.toString()
                layout.setText(font, digit)
                font.color.set(0.92f, 0.98f, 1f, 1f)
                font.draw(batch, layout, cx - layout.width * 0.5f, cy + layout.height * 0.38f)

                smallFont.color.set(0.55f, 0.82f, 1f, 0.75f)
                layoutSmall.setText(smallFont, "WARP")
                smallFont.draw(
                    batch, layoutSmall,
                    cx - layoutSmall.width * 0.5f,
                    cy - radius * 0.42f,
                )
            }
            WarpSystem.State.ACTIVE -> {
                layout.setText(font, "WARP")
                font.color.set(0.82f, 0.68f, 1f, 0.95f)
                font.draw(batch, layout, cx - layout.width * 0.5f, cy + layout.height * 0.38f)

                smallFont.color.set(0.70f, 0.58f, 1f, 0.70f)
                layoutSmall.setText(smallFont, "STOP")
                smallFont.draw(
                    batch, layoutSmall,
                    cx - layoutSmall.width * 0.5f,
                    cy - radius * 0.42f,
                )
            }
            else -> {
                layout.setText(font, "WARP")
                font.color.set(0.72f, 0.90f, 1f, 0.82f)
                font.draw(batch, layout, cx - layout.width * 0.5f, cy + layout.height * 0.38f)

                smallFont.color.set(0.48f, 0.72f, 0.92f, 0.55f)
                layoutSmall.setText(smallFont, "3s")
                smallFont.draw(
                    batch, layoutSmall,
                    cx - layoutSmall.width * 0.5f,
                    cy - radius * 0.42f,
                )
            }
        }

        if (warp.hudMessage != null && warp.messageAlpha > 0.05f) {
            smallFont.color.set(1f, 0.42f, 0.32f, warp.messageAlpha * 0.9f)
            layoutSmall.setText(smallFont, "ZU NAH")
            smallFont.draw(
                batch, layoutSmall,
                cx - layoutSmall.width * 0.5f,
                cy - radius * 1.55f,
            )
        }
    }
}

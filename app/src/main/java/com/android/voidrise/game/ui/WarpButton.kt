package com.android.voidrise.game.ui

import com.android.voidrise.game.warp.WarpSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class WarpButton {

    var cx = 0f
    var cy = 0f
    val radius = 58f

    private val layout = GlyphLayout()
    private var wasTouched = false

    fun layout(screenW: Int, screenH: Int) {
        cx = screenW * 0.50f
        cy = radius + 36f
    }

    fun update(warp: WarpSystem, shipPos: com.badlogic.gdx.math.Vector3) {
        var touched = false
        for (i in 0 until minOf(Gdx.input.maxPointers, 5)) {
            if (!Gdx.input.isTouched(i)) continue
            val tx = Gdx.input.getX(i).toFloat()
            val ty = Gdx.graphics.height - Gdx.input.getY(i).toFloat()
            if (Vector2(tx - cx, ty - cy).len() <= radius * 1.45f) {
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

    fun drawFilled(shapes: ShapeRenderer, warp: WarpSystem, @Suppress("UNUSED_PARAMETER") time: Float) {
        val active = warp.state == WarpSystem.State.ACTIVE
        val charging = warp.state == WarpSystem.State.COUNTDOWN

        val r = when {
            active   -> Color(0.55f, 0.35f, 1f, 0.85f)
            charging -> Color(0.25f, 0.85f, 1f, 0.75f)
            else     -> Color(0.20f, 0.55f, 1f, 0.50f)
        }

        shapes.color.set(r)
        shapes.circle(cx, cy, radius)
        shapes.color.set(r.r, r.g, r.b, 0.12f)
        shapes.circle(cx, cy, radius - 6f)
    }

    fun drawLabel(batch: SpriteBatch, font: BitmapFont, warp: WarpSystem) {
        val label = when (warp.state) {
            WarpSystem.State.ACTIVE    -> "STOP"
            WarpSystem.State.COUNTDOWN -> "STOP"
            else                       -> "WARP"
        }
        layout.setText(font, label)
        font.color.set(1f, 1f, 1f, if (warp.state == WarpSystem.State.IDLE) 0.75f else 1f)
        font.draw(batch, layout, cx - layout.width * 0.5f, cy + layout.height * 0.35f)
    }
}

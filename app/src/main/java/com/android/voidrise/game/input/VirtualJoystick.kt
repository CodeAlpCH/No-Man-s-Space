package com.android.voidrise.game.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.min

/**
 * Dynamischer Joystick – Basis erscheint dort wo du tippst (linke Hälfte).
 * Sci-Fi Targeting-Style: kein fülliges Kreis-UI, nur geometrische Marker.
 *
 * Koordinaten: Screen-Pixel mit (0,0) unten-links.
 */
class VirtualJoystick(
    private val baseRadius: Float = 110f,
    private val knobRadius: Float = 22f,
) {
    // Dynamisch: baseCenter ändert sich bei jedem neuen Touch
    private val baseCenter   = Vector2()
    private val defaultBase  = Vector2()
    private val knobPosition = Vector2()
    private val output       = Vector2()
    private var pointerId    = -1
    private var active       = false

    // Smooth visual pulse
    private var pulseTimer   = 0f

    fun layout(screenW: Int, screenH: Int) {
        defaultBase.set(screenW * 0.78f, screenH * 0.18f)
        if (!active) {
            baseCenter.set(defaultBase)
            knobPosition.set(defaultBase)
        }
    }

    fun update(delta: Float) {
        pulseTimer = (pulseTimer + delta * 1.8f) % MathUtils.PI2

        if (pointerId == -1) capturePointer()
        else if (!Gdx.input.isTouched(pointerId)) release()
        else drag(flipY(Gdx.input.getX(pointerId).toFloat(), Gdx.input.getY(pointerId).toFloat()))
    }

    private fun capturePointer() {
        for (i in 0 until min(Gdx.input.maxPointers, 5)) {
            if (!Gdx.input.isTouched(i)) continue
            val pos = flipY(Gdx.input.getX(i).toFloat(), Gdx.input.getY(i).toFloat())
            if (pos.x < Gdx.graphics.width * 0.48f) continue   // only right half
            pointerId = i
            active    = true
            baseCenter.set(pos)     // ← dynamic: base jumps to touch start
            knobPosition.set(pos)
            drag(pos)
            return
        }
    }

    private fun drag(pos: Vector2) {
        val dx      = pos.x - baseCenter.x
        val dy      = pos.y - baseCenter.y
        val dist    = Vector2(dx, dy).len()
        val maxDist = baseRadius - knobRadius

        if (dist > maxDist) {
            val s = maxDist / dist
            knobPosition.set(baseCenter.x + dx * s, baseCenter.y + dy * s)
            output.set(dx / dist, dy / dist)
        } else if (dist > 0.5f) {
            knobPosition.set(pos)
            output.set(dx / maxDist, dy / maxDist)
        } else {
            knobPosition.set(baseCenter)
            output.setZero()
        }
    }

    private fun release() {
        pointerId = -1
        active    = false
        baseCenter.set(defaultBase)
        knobPosition.set(defaultBase)
        output.setZero()
    }

    private fun flipY(x: Float, rawY: Float) =
        Vector2(x, Gdx.graphics.height - rawY)

    fun direction(): Vector2 = output
    fun isActive():  Boolean = active

    // ─── Draw ─────────────────────────────────────────────────────────────────

    fun draw(shapes: ShapeRenderer) {
        val bx = baseCenter.x
        val by = baseCenter.y

        if (active) {
            drawActive(shapes, bx, by)
        } else {
            drawIdle(shapes, bx, by)
        }
    }

    /**
     * Inactive: minimal targeting crosshair at default position.
     * Looks like a HUD marker / aiming reticle.
     */
    private fun drawIdle(shapes: ShapeRenderer, bx: Float, by: Float) {
        val pulse = MathUtils.sin(pulseTimer) * 0.5f + 0.5f   // 0..1
        val alpha = 0.10f + pulse * 0.06f

        // 4 outward tick marks (crosshair)
        drawCrosshairTicks(shapes, bx, by, alpha, 18f, 10f)

        // Tiny center dot
        shapes.color.set(0.5f, 0.9f, 1f, alpha + 0.04f)
        shapes.circle(bx, by, 3.5f)
    }

    /**
     * Active: ghost ring at base + direction line + precision knob.
     */
    private fun drawActive(shapes: ShapeRenderer, bx: Float, by: Float) {
        // Ghost boundary ring
        shapes.color.set(0.4f, 0.85f, 1f, 0.08f)
        shapes.circle(bx, by, baseRadius)

        // Inner ring (dead zone marker at 25%)
        shapes.color.set(0.4f, 0.85f, 1f, 0.05f)
        shapes.circle(bx, by, baseRadius * 0.25f)

        // Crosshair ticks at base center
        drawCrosshairTicks(shapes, bx, by, 0.18f, 14f, 8f)

        // Direction line: base → knob
        val dx    = knobPosition.x - bx
        val dy    = knobPosition.y - by
        val dist  = Vector2(dx, dy).len()
        if (dist > 4f) {
            shapes.color.set(0.5f, 0.92f, 1f, 0.32f)
            shapes.rectLine(bx, by, knobPosition.x, knobPosition.y, 1.5f)
        }

        // Knob outer glow
        val intensity = output.len()
        shapes.color.set(0.38f, 0.80f, 1f, 0.22f + intensity * 0.15f)
        shapes.circle(knobPosition.x, knobPosition.y, knobRadius * 1.6f)

        // Knob ring
        shapes.color.set(0.55f, 0.92f, 1f, 0.50f + intensity * 0.20f)
        shapes.circle(knobPosition.x, knobPosition.y, knobRadius)

        // Knob center pip
        shapes.color.set(1f, 1f, 1f, 0.85f)
        shapes.circle(knobPosition.x, knobPosition.y, 5f)

        // Directional chevrons: show which direction is active
        drawDirectionChevrons(shapes, bx, by, output)
    }

    /** 4 short tick marks arranged as a targeting crosshair. */
    private fun drawCrosshairTicks(
        shapes: ShapeRenderer, cx: Float, cy: Float,
        alpha: Float, outerR: Float, innerR: Float,
    ) {
        val thick = 1.5f
        shapes.color.set(0.45f, 0.88f, 1f, alpha)
        // North
        shapes.rectLine(cx, cy + innerR, cx, cy + outerR, thick)
        // South
        shapes.rectLine(cx, cy - innerR, cx, cy - outerR, thick)
        // East
        shapes.rectLine(cx + innerR, cy, cx + outerR, cy, thick)
        // West
        shapes.rectLine(cx - innerR, cy, cx - outerR, cy, thick)
        // Corner dots
        shapes.color.set(0.45f, 0.88f, 1f, alpha * 0.6f)
        shapes.circle(cx + outerR * 0.68f, cy + outerR * 0.68f, 1.5f)
        shapes.circle(cx - outerR * 0.68f, cy + outerR * 0.68f, 1.5f)
        shapes.circle(cx + outerR * 0.68f, cy - outerR * 0.68f, 1.5f)
        shapes.circle(cx - outerR * 0.68f, cy - outerR * 0.68f, 1.5f)
    }

    /** Small chevrons at N/S/E/W that illuminate when pushing in that direction. */
    private fun drawDirectionChevrons(
        shapes: ShapeRenderer, cx: Float, cy: Float, dir: Vector2,
    ) {
        val cr   = baseRadius * 0.72f
        val size = 8f
        val dirs = arrayOf(
            floatArrayOf(0f, 1f, cx, cy + cr),      // N
            floatArrayOf(0f, -1f, cx, cy - cr),     // S
            floatArrayOf(1f, 0f, cx + cr, cy),      // E
            floatArrayOf(-1f, 0f, cx - cr, cy),     // W
        )
        for (d in dirs) {
            val dot   = dir.x * d[0] + dir.y * d[1]
            val alpha = (0.06f + dot.coerceAtLeast(0f) * 0.55f)
            val dx    = d[0]; val dy = d[1]
            val bx2   = d[2]; val by2 = d[3]
            // Draw a small chevron (2 lines forming a ">" or "^")
            val px   = -dy; val py = dx   // perpendicular
            shapes.color.set(0.45f, 0.92f, 1f, alpha)
            shapes.rectLine(bx2 - px * size - dx * size, by2 - py * size - dy * size,
                            bx2,                         by2,                         1.5f)
            shapes.rectLine(bx2 + px * size - dx * size, by2 + py * size - dy * size,
                            bx2,                         by2,                         1.5f)
        }
    }
}

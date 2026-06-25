package com.android.voidrise.game.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import kotlin.math.min

/**
 * Professional aircraft-style throttle lever for the left side of the screen.
 *
 * Unlike a spring-back joystick, the throttle HOLDS its position when released.
 * Drag up = more thrust (0→1), drag down = less thrust.
 *
 * Touch zone: left 22% of screen width.
 */
class ThrottleLever {

    var value = 0f          // 0 = engines off, 1 = full thrust
        private set

    // Layout (set in layout())
    private var cx         = 0f   // center X of the lever
    private var trackBot   = 0f   // Y of 0% throttle
    private var trackTop   = 0f   // Y of 100% throttle
    private var touchZoneW = 0f   // X width for touch capture

    private var activePtrIdx = -1
    private var pulseTimer   = 0f

    fun layout(screenW: Int, screenH: Int) {
        cx         = screenW * 0.095f
        trackBot   = screenH * 0.07f
        trackTop   = screenH * 0.52f
        touchZoneW = screenW * 0.22f
    }

    fun update(delta: Float) {
        pulseTimer = (pulseTimer + delta * 1.5f) % MathUtils.PI2

        val h = Gdx.graphics.height.toFloat()

        // Check if active pointer was released
        if (activePtrIdx != -1 && !Gdx.input.isTouched(activePtrIdx)) {
            activePtrIdx = -1
        }

        // Try to capture a new touch on the left side
        if (activePtrIdx == -1) {
            for (i in 0 until min(Gdx.input.maxPointers, 5)) {
                if (!Gdx.input.isTouched(i)) continue
                val tx = Gdx.input.getX(i).toFloat()
                val ty = h - Gdx.input.getY(i).toFloat()
                if (tx <= touchZoneW) {
                    activePtrIdx = i
                    break
                }
            }
        }

        // Update value from active pointer
        if (activePtrIdx != -1 && Gdx.input.isTouched(activePtrIdx)) {
            val ty = h - Gdx.input.getY(activePtrIdx).toFloat()
            value = ((ty - trackBot) / (trackTop - trackBot)).coerceIn(0f, 1f)
        }
    }

    // ─── Draw ─────────────────────────────────────────────────────────────────

    fun draw(shapes: ShapeRenderer) {
        val pulse = MathUtils.sin(pulseTimer) * 0.5f + 0.5f
        val knobY = trackBot + value * (trackTop - trackBot)
        val trackW = 6f
        val knobW = 44f
        val knobH = 22f

        // ── Track background ──────────────────────────────────────────────────
        shapes.color.set(0.10f, 0.12f, 0.22f, 0.50f)
        shapes.rectLine(cx, trackBot, cx, trackTop, trackW)

        // ── Active portion (below knob = filled) ──────────────────────────────
        val cr = 0.25f + value * 0.75f
        val cg = 0.65f + value * 0.30f
        shapes.color.set(cr, cg, 1f, 0.80f)
        if (knobY > trackBot + 2f) {
            shapes.rectLine(cx, trackBot, cx, knobY, trackW + 2f)
        }

        // ── Tick marks at 25% / 50% / 75% ────────────────────────────────────
        for (frac in floatArrayOf(0.25f, 0.50f, 0.75f)) {
            val ty = trackBot + frac * (trackTop - trackBot)
            val tickAlpha = if (value >= frac) 0.55f else 0.18f
            shapes.color.set(0.60f, 0.85f, 1f, tickAlpha)
            shapes.rectLine(cx - 10f, ty, cx + 10f, ty, 1.5f)
        }

        // ── Knob ──────────────────────────────────────────────────────────────
        // Outer glow
        shapes.color.set(cr, cg, 1f, 0.18f + pulse * 0.08f)
        shapes.rect(cx - knobW * 0.5f - 6f, knobY - knobH * 0.5f - 4f,
                    knobW + 12f, knobH + 8f)

        // Main knob body
        shapes.color.set(0.15f, 0.20f, 0.38f, 0.90f)
        shapes.rect(cx - knobW * 0.5f, knobY - knobH * 0.5f, knobW, knobH)

        // Knob top edge (bright accent line)
        shapes.color.set(cr, cg, 1f, 0.95f)
        shapes.rectLine(cx - knobW * 0.5f + 4f, knobY + knobH * 0.5f - 3f,
                        cx + knobW * 0.5f - 4f, knobY + knobH * 0.5f - 3f, 2f)

        // Grip lines (3 horizontal lines across knob center)
        shapes.color.set(0.50f, 0.75f, 1f, 0.45f)
        for (g in -1..1) {
            val gy = knobY + g * 5f
            shapes.rectLine(cx - knobW * 0.4f, gy, cx + knobW * 0.4f, gy, 1.5f)
        }

        // ── MIN / MAX labels at ends ───────────────────────────────────────────
        // (just horizontal markers instead of text)
        shapes.color.set(0.35f, 0.65f, 1f, 0.30f)
        shapes.rectLine(cx - 14f, trackBot,  cx + 14f, trackBot,  1.5f)
        shapes.rectLine(cx - 14f, trackTop,  cx + 14f, trackTop,  1.5f)

        // Full-thrust arrow marker at top
        val ay = trackTop + 12f
        shapes.color.set(1f, 0.70f, 0.20f, 0.45f)
        shapes.triangle(cx, ay + 8f,  cx - 9f, ay,  cx + 9f, ay)
    }
}

package com.android.voidrise.game.ui

import com.android.voidrise.game.warp.WarpSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import kotlin.math.min

/**
 * Warp button — visual reference: preview.html (energy ring, reactor core, countdown tick).
 * Optional [PATH_OFF] / [PATH_ON] PNGs used in idle/active when present.
 */
class WarpButton {

    var cx = 0f
        private set
    var cy = 0f
        private set
    /** Outer radius (~95 px on a 190 px button in the HTML mock). */
    var radius = 72f
        private set

    private val layout = GlyphLayout()
    private val drawMatrix = Matrix4()
    private var wasTouched = false

    private var texOff: Texture? = null
    private var texOn: Texture? = null
    private var regionOff: TextureRegion? = null
    private var regionOn: TextureRegion? = null
    private var hasArt = false

    private var ringAngle = 0f
    private var lastCountdownDigit = -1
    private var tickTimer = 0f
    private var burstTimer = 0f
    private var flashTimer = 0f
    private var prevWarpState = WarpSystem.State.IDLE

    fun load() {
        dispose()
        if (!Gdx.files.internal(PATH_OFF).exists()) return
        texOff = Texture(Gdx.files.internal(PATH_OFF)).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        regionOff = TextureRegion(texOff)
        if (Gdx.files.internal(PATH_ON).exists()) {
            texOn = Texture(Gdx.files.internal(PATH_ON)).apply {
                setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
            regionOn = TextureRegion(texOn)
        } else {
            regionOn = regionOff
        }
        hasArt = true
    }

    fun dispose() {
        texOff?.dispose()
        texOn?.takeIf { it !== texOff }?.dispose()
        texOff = null
        texOn = null
        regionOff = null
        regionOn = null
        hasArt = false
    }

    fun layout(screenW: Int, screenH: Int) {
        cx = screenW * 0.50f
        cy = screenH * 0.108f
        radius = (screenH * 0.048f).coerceIn(68f, 98f)
    }

    fun updateAnim(dt: Float, warp: WarpSystem) {
        when (warp.state) {
            WarpSystem.State.ACTIVE -> ringAngle += dt * 90f
            else -> ringAngle = MathUtils.lerp(ringAngle, 0f, (dt * 4f).coerceAtMost(1f))
        }

        if (warp.state == WarpSystem.State.COUNTDOWN) {
            if (warp.countdownDigit != lastCountdownDigit) {
                lastCountdownDigit = warp.countdownDigit
                tickTimer = 0f
            }
            tickTimer = (tickTimer + dt).coerceAtMost(TICK_DURATION)
        } else {
            lastCountdownDigit = -1
            tickTimer = TICK_DURATION
        }

        if (prevWarpState == WarpSystem.State.COUNTDOWN && warp.state == WarpSystem.State.ACTIVE) {
            burstTimer = 0.95f
            flashTimer = 0.55f
        }
        burstTimer = (burstTimer - dt).coerceAtLeast(0f)
        flashTimer = (flashTimer - dt).coerceAtLeast(0f)

        prevWarpState = warp.state
    }

    fun update(warp: WarpSystem, shipPos: com.badlogic.gdx.math.Vector3) {
        var touched = false
        val hitR = radius * 1.12f
        for (i in 0 until min(Gdx.input.maxPointers, 5)) {
            if (!Gdx.input.isTouched(i)) continue
            val tx = Gdx.input.getX(i).toFloat()
            val ty = Gdx.graphics.height - Gdx.input.getY(i).toFloat()
            if (Vector2(tx - cx, ty - cy).len() <= hitR) {
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
        val charging = warp.state == WarpSystem.State.COUNTDOWN
        val active = warp.state == WarpSystem.State.ACTIVE
        val idle = !charging && !active

        if (burstTimer > 0f) drawShockwaves(shapes, burstTimer)

        val chargePulse = if (charging) {
            MathUtils.sin(time * MathUtils.PI2 / 0.8f) * 0.5f + 0.5f
        } else 0f
        val activeGlow = if (active) MathUtils.sin(time * MathUtils.PI2 / 1.8f) * 0.5f + 0.5f else 0f

        drawOuterGlow(shapes, charging, active, chargePulse, activeGlow)

        if (idle && hasArt) {
            // PNG handles idle look; core drawn in sprite pass
            return
        }

        drawEnergyRing(shapes, charging, active, chargePulse, activeGlow)
        drawCore(shapes, charging, active, flashTimer)
        drawCoreHighlight(shapes, charging, active)
    }

    fun drawLines(shapes: ShapeRenderer, warp: WarpSystem, @Suppress("UNUSED_PARAMETER") time: Float) {
        val charging = warp.state == WarpSystem.State.COUNTDOWN
        val active = warp.state == WarpSystem.State.ACTIVE
        if (charging || active || !hasArt) {
            val coreR = radius * CORE_R
            val innerR = coreR * 0.68f
            shapes.color.set(
                if (active) 0.96f else 0.41f,
                if (active) 0.72f else 0.88f,
                1f,
                if (active) 0.80f else 0.45f,
            )
            shapes.circle(cx, cy, coreR)
            shapes.color.set(
                if (active) 0.96f else 0.41f,
                if (active) 0.72f else 0.88f,
                1f,
                if (active) 0.55f else 0.35f,
            )
            shapes.circle(cx, cy, innerR)
        }
    }

    fun drawSprite(batch: SpriteBatch, warp: WarpSystem) {
        if (warp.state != WarpSystem.State.IDLE || !hasArt) return
        val size = radius * 2f
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(regionOff!!, cx - radius, cy - radius, size, size)
    }

    fun drawLabel(batch: SpriteBatch, countdownFont: BitmapFont, warpFont: BitmapFont, warp: WarpSystem) {
        when (warp.state) {
            WarpSystem.State.COUNTDOWN -> drawCountdownDigit(batch, countdownFont, warp.countdownDigit)
            WarpSystem.State.ACTIVE -> drawWarpCaption(batch, warpFont, active = true)
            else -> {
                if (!hasArt) drawWarpCaption(batch, warpFont, active = false)
            }
        }

        if (warp.hudMessage != null && warp.messageAlpha > 0.05f) {
            layout.setText(warpFont, "ZU NAH")
            warpFont.color.set(1f, 0.38f, 0.28f, warp.messageAlpha * 0.95f)
            warpFont.draw(batch, layout, cx - layout.width * 0.5f, cy - radius * 1.45f)
        }
    }

    // ─── Layers (preview.html) ────────────────────────────────────────────────

    private fun drawOuterGlow(
        shapes: ShapeRenderer,
        charging: Boolean,
        active: Boolean,
        chargePulse: Float,
        activeGlow: Float,
    ) {
        if (!charging && !active) return
        val r = radius * 1.28f
        val a = when {
            charging -> 0.14f + chargePulse * 0.22f
            else     -> 0.16f + activeGlow * 0.18f
        }
        shapes.color.set(
            if (active) 0.55f else 0.19f,
            if (active) 0.24f else 0.82f,
            1f,
            a,
        )
        shapes.circle(cx, cy, r)
    }

    private fun drawEnergyRing(
        shapes: ShapeRenderer,
        charging: Boolean,
        active: Boolean,
        chargePulse: Float,
        activeGlow: Float,
    ) {
        val rOuter = radius
        val rInner = radius * RING_INNER
        val segments = 48
        val step = 360f / segments

        for (i in 0 until segments) {
            val t = i / segments.toFloat()
            val (cr, cg, cb) = if (active) activeRingColor(t) else idleRingColor(t)
            val bright = when {
                charging -> 1f + chargePulse * 0.45f
                active   -> 1f + activeGlow * 0.35f
                else     -> 1f
            }
            shapes.color.set(
                (cr * bright).coerceAtMost(1f),
                (cg * bright).coerceAtMost(1f),
                (cb * bright).coerceAtMost(1f),
                if (charging) 0.92f else 0.88f,
            )
            val start = ringAngle + i * step
            shapes.arc(cx, cy, rOuter, start, step * 0.94f)
        }

        // Dark separator — masks center so only the ring band stays visible
        shapes.color.set(0.01f, 0.02f, 0.05f, 1f)
        shapes.circle(cx, cy, rInner)
    }

    private fun drawCore(
        shapes: ShapeRenderer,
        charging: Boolean,
        active: Boolean,
        flashTimer: Float,
    ) {
        val coreR = radius * CORE_R
        val flash = if (flashTimer > 0f) {
            val t = 1f - flashTimer / 0.55f
            if (t < 0.35f) MathUtils.lerp(1f, 2.4f, t / 0.35f) else MathUtils.lerp(2.4f, 1f, (t - 0.35f) / 0.65f)
        } else 1f

        if (active) {
            shapes.color.set(0.65f * flash, 0.25f * flash, 0.91f * flash, 1f)
            shapes.circle(cx, cy, coreR * 0.55f)
            shapes.color.set(0.31f * flash, 0.07f * flash, 0.60f * flash, 1f)
            shapes.circle(cx, cy, coreR * 0.82f)
            shapes.color.set(0.09f * flash, 0.02f * flash, 0.18f * flash, 1f)
        } else {
            shapes.color.set(0.09f * flash, 0.22f * flash, 0.42f * flash, 1f)
            shapes.circle(cx, cy, coreR * 0.45f)
            shapes.color.set(0.03f * flash, 0.07f * flash, 0.15f * flash, 1f)
            shapes.circle(cx, cy, coreR * 0.78f)
            shapes.color.set(0.01f * flash, 0.02f * flash, 0.04f * flash, 1f)
        }
        shapes.circle(cx, cy, coreR)

        if (charging) {
            shapes.color.set(0.22f, 0.84f, 1f, 0.12f + MathUtils.sin(tickTimer * 12f) * 0.04f)
            shapes.circle(cx, cy, coreR * 0.92f)
        }
    }

    private fun drawCoreHighlight(shapes: ShapeRenderer, charging: Boolean, active: Boolean) {
        if (charging) return
        val w = radius * 0.50f
        val h = radius * 0.22f
        val hx = cx - w * 0.5f
        val hy = cy + radius * 0.28f
        shapes.color.set(
            if (active) 0.95f else 0.57f,
            if (active) 0.75f else 0.91f,
            1f,
            if (active) 0.14f else 0.12f,
        )
        shapes.ellipse(hx, hy, w, h)
    }

    private fun drawShockwaves(shapes: ShapeRenderer, burstTimer: Float) {
        val t = 1f - burstTimer / 0.95f
        for (delay in floatArrayOf(0f, 0.18f)) {
            val lt = ((t - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (lt <= 0f) continue
            val scale = MathUtils.lerp(0.85f, 1.65f, lt)
            val alpha = MathUtils.lerp(0.9f, 0f, lt)
            shapes.color.set(0.89f, 0.42f, 1f, alpha * 0.85f)
            shapes.circle(cx, cy, radius * scale)
        }
    }

    private fun drawCountdownDigit(batch: SpriteBatch, font: BitmapFont, digit: Int) {
        val text = digit.toString()
        layout.setText(font, text)
        val scale = tickScale()
        font.color.set(1f, 1f, 1f, 1f)

        val old = batch.transformMatrix.cpy()
        drawMatrix.set(old)
        drawMatrix.translate(cx, cy, 0f)
        drawMatrix.scale(scale, scale, 1f)
        drawMatrix.translate(-cx, -cy, 0f)
        batch.transformMatrix = drawMatrix

        font.draw(
            batch, layout,
            cx - layout.width * 0.5f,
            cy + layout.height * 0.38f,
        )
        batch.transformMatrix = old
    }

    private fun drawWarpCaption(batch: SpriteBatch, font: BitmapFont, active: Boolean) {
        layout.setText(font, "WARP")
        font.color.set(1f, 1f, 1f, if (active) 1f else 0.95f)
        font.draw(
            batch, layout,
            cx - layout.width * 0.5f,
            cy + layout.height * 0.35f,
        )
    }

    /** preview.html countdownTick: 0.7 → 1.12 → 1.0 over 650 ms */
    private fun tickScale(): Float {
        val t = (tickTimer / TICK_DURATION).coerceIn(0f, 1f)
        return when {
            t < 0.45f -> MathUtils.lerp(0.7f, 1.12f, t / 0.45f)
            else      -> MathUtils.lerp(1.12f, 1f, (t - 0.45f) / 0.55f)
        }
    }

    private fun idleRingColor(t: Float): Triple<Float, Float, Float> {
        val angle = (t * 360f + ringAngle) % 360f
        return when {
            angle < 90f  -> Triple(0.26f, 0.91f, 1f)
            angle < 180f -> Triple(0.14f, 0.41f, 1f)
            angle < 270f -> Triple(0.06f, 0.17f, 0.46f)
            else         -> Triple(0.26f, 0.91f, 1f)
        }
    }

    private fun activeRingColor(t: Float): Triple<Float, Float, Float> {
        val angle = (t * 360f + ringAngle) % 360f
        return when {
            angle < 120f -> Triple(0.44f, 0.22f, 1f)
            angle < 240f -> Triple(0.87f, 0.38f, 1f)
            else         -> Triple(0.22f, 0.13f, 0.64f)
        }
    }

    companion object {
        const val PATH_OFF = "ui/warp_off.png"
        const val PATH_ON  = "ui/warp_on.png"

        private const val TICK_DURATION = 0.65f
        /** 83 / 95 — warp-core inset from preview.html */
        private const val CORE_R = 0.874f
        /** 88 / 95 — energy-ring dark inner edge */
        private const val RING_INNER = 0.926f
    }
}

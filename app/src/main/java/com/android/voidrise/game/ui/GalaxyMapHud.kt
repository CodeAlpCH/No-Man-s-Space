package com.android.voidrise.game.ui

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.math.sqrt

/**
 * Round heading-up radar — top of disc = where the ship is facing.
 * No square frame; blips clamp to the rim when out of range.
 */
object GalaxyMapHud {

    /** World units visible from center to rim. */
    private const val MAP_RANGE = 220_000f

    /** Fixed nav stars in world XZ (sparse universe landmarks). */
    private val NAV_STARS_XZ = floatArrayOf(
        -55_000f, -95_000f,
         72_000f, -140_000f,
        -30_000f, -175_000f,
        110_000f,  -60_000f,
        -90_000f, -155_000f,
         40_000f, -210_000f,
    )

    private val rel     = Vector3()
    private val starPos = Vector3()

    fun draw(
        shapes: ShapeRenderer,
        sw: Float,
        sh: Float,
        shipPos: Vector3,
        shipRight: Vector3,
        shipForward: Vector3,
        planetPositions: List<Vector3>,
        time: Float,
        linePass: Boolean,
    ) {
        val radius = (sw * 0.11f).coerceIn(58f, 82f)
        val pad    = 16f
        val cx     = sw - pad - radius
        val cy     = sh - pad - radius
        val inner  = radius - 3f

        if (!linePass) {
            drawFilledDisc(shapes, cx, cy, radius, inner, time)
            planetPositions.forEach { planetPos ->
                drawBlip(shapes, cx, cy, inner, shipPos, shipRight, shipForward, planetPos, 5.5f, true, time)
            }
            for (i in NAV_STARS_XZ.indices step 2) {
                starPos.set(NAV_STARS_XZ[i], shipPos.y, NAV_STARS_XZ[i + 1])
                drawBlip(
                    shapes, cx, cy, inner, shipPos, shipRight, shipForward,
                    starPos, 1.8f, false, time,
                )
            }
            drawShipWedge(shapes, cx, cy)
        } else {
            drawRingOutline(shapes, cx, cy, radius, inner)
        }
    }

    private fun drawFilledDisc(
        shapes: ShapeRenderer,
        cx: Float, cy: Float,
        radius: Float, inner: Float,
        time: Float,
    ) {
        shapes.color.set(0.03f, 0.08f, 0.18f, 0.55f)
        shapes.circle(cx, cy, radius)
        shapes.color.set(0.08f, 0.20f, 0.38f, 0.35f)
        shapes.circle(cx, cy, inner)

        for (ring in 1..3) {
            val r = inner * ring / 3f
            shapes.color.set(0.22f, 0.55f, 0.92f, 0.06f + ring * 0.025f)
            shapes.circle(cx, cy, r)
        }

        // Slow sweep — alive universe feel
        val sweep = time * 0.45f
        for (i in 0 until 18) {
            val a  = sweep + i * (MathUtils.PI2 / 18f)
            val sx = cx + MathUtils.cos(a) * inner * 0.88f
            val sy = cy + MathUtils.sin(a) * inner * 0.88f
            shapes.color.set(0.65f, 0.85f, 1f, 0.12f + (i % 3) * 0.06f)
            shapes.circle(sx, sy, 1.1f)
        }
    }

    private fun drawRingOutline(
        shapes: ShapeRenderer,
        cx: Float, cy: Float,
        radius: Float, inner: Float,
    ) {
        shapes.color.set(0.30f, 0.72f, 1.0f, 0.55f)
        shapes.circle(cx, cy, radius)
        shapes.color.set(0.18f, 0.48f, 0.85f, 0.28f)
        shapes.circle(cx, cy, inner)
    }

    /** Ship at center — wedge points up (= forward on heading-up radar). */
    private fun drawShipWedge(shapes: ShapeRenderer, cx: Float, cy: Float) {
        shapes.color.set(0.55f, 0.95f, 1.0f, 0.95f)
        shapes.circle(cx, cy, 3f)
        shapes.triangle(
            cx, cy + 11f,
            cx - 5f, cy - 4f,
            cx + 5f, cy - 4f,
        )
    }

    private fun drawBlip(
        shapes: ShapeRenderer,
        cx: Float, cy: Float, inner: Float,
        shipPos: Vector3,
        shipRight: Vector3,
        shipForward: Vector3,
        worldPos: Vector3,
        dotR: Float,
        isPlanet: Boolean,
        time: Float,
    ) {
        rel.set(worldPos).sub(shipPos)
        val localX = rel.dot(shipRight)
        val localY = rel.dot(shipForward)
        val scale  = inner / MAP_RANGE

        var px = cx + localX * scale
        var py = cy + localY * scale

        val offX = px - cx
        val offY = py - cy
        val offLen = sqrt(offX * offX + offY * offY)
        val maxR = inner - 8f
        if (offLen > maxR) {
            val s = maxR / offLen
            px = cx + offX * s
            py = cy + offY * s
        }

        if (isPlanet) {
            val pulse = MathUtils.sin(time * 2.2f) * 0.15f + 0.85f
            shapes.color.set(0.12f, 0.40f, 1.0f, 0.30f * pulse)
            shapes.circle(px, py, dotR + 4f)
            shapes.color.set(0.35f, 0.78f, 1.0f, 0.95f)
            shapes.circle(px, py, dotR)
        } else {
            shapes.color.set(0.70f, 0.82f, 1f, 0.35f)
            shapes.circle(px, py, dotR)
        }
    }
}

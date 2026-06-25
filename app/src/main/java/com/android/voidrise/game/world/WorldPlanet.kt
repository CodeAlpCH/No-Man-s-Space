package com.android.voidrise.game.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3

/**
 * Single hand-placed world — NMS scale: enormous radius, absurd distance, still fills the sky.
 *
 * From start (0, 30, -380) facing -Z:
 *   ~186 000 units away, ~24° apparent diameter (ocean giant on the horizon).
 */
object WorldPlanet {

    const val NAME   = "Kepler"
    const val RADIUS = 42_000f

    /** Full 3D mesh + planet.frag below this; cheap billboard impostor above. */
    const val MESH_LOD_SURFACE_KM = 1_500f

    /** Warp auto-stops and “too close” warnings use this (atmosphere entry). */
    const val ATMOSPHERE_SURFACE_KM = 1_000f

    val position = Vector3(14_000f, 6_000f, -188_000f)

    const val SEED  = 0.04f   // arch 0 = Ocean (planet.frag)
    const val TYPE  = 3       // PLANET

    val baseColor = Color(0.10f, 0.32f, 0.88f, 1f)
    val glowColor = Color(0.38f, 0.68f, 1.00f, 0.8f)

    /** Straight-line distance from a world position. */
    fun distanceFrom(from: Vector3): Float = from.dst(position)

    fun surfaceDistance(from: Vector3): Float =
        (distanceFrom(from) - RADIUS).coerceAtLeast(0f)

    /** Apparent angular radius in degrees (for debug / HUD). */
    fun apparentDiameterDeg(from: Vector3): Float {
        val d = distanceFrom(from).coerceAtLeast(1f)
        return Math.toDegrees(2.0 * kotlin.math.atan((RADIUS / d).toDouble())).toFloat()
    }
}

package com.android.voidrise.game.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3

object WorldPlanet {

    data class Planet(
        val name: String,
        val position: Vector3,
        val radius: Float,
        val seed: Float,
        val type: Int,
        val baseColor: Color,
        val glowColor: Color,
    )

    val kepler = Planet(
        name = "Kepler",
        position = Vector3(14_000f, 6_000f, -188_000f),
        radius = 42_000f,
        seed = 0.04f,   // arch 0 = Ocean
        type = 3,
        baseColor = Color(0.10f, 0.32f, 0.88f, 1f),
        glowColor = Color(0.38f, 0.68f, 1.00f, 0.8f),
    )

    val terraNova = Planet(
        name = "Terra Nova",
        position = Vector3(104_000f, 24_000f, -420_000f),
        radius = 8_400f,
        seed = 0.24f,   // arch 1 = Earth-like terrain
        type = 3,
        baseColor = Color(0.08f, 0.30f, 0.60f, 1f),
        glowColor = Color(0.42f, 0.70f, 1.00f, 0.75f),
    )

    val planets = listOf(kepler, terraNova)

    const val NAME   = "Kepler"
    const val RADIUS = 42_000f

    /** Full 3D mesh + planet.frag below this; cheap billboard impostor above. */
    const val MESH_LOD_SURFACE_KM = 1_500f

    /** Warp auto-stops and “too close” warnings use this (atmosphere entry). */
    const val ATMOSPHERE_SURFACE_KM = 1_000f

    val position: Vector3 = kepler.position

    const val SEED  = 0.04f   // arch 0 = Ocean (planet.frag)
    const val TYPE  = 3       // PLANET

    val baseColor: Color = kepler.baseColor
    val glowColor: Color = kepler.glowColor

    /** Straight-line distance from a world position. */
    fun distanceFrom(from: Vector3): Float = from.dst(position)

    fun surfaceDistance(from: Vector3): Float =
        surfaceDistance(from, nearestPlanet(from))

    fun surfaceDistance(from: Vector3, planet: Planet): Float =
        (from.dst(planet.position) - planet.radius).coerceAtLeast(0f)

    fun nearestPlanet(from: Vector3): Planet =
        planets.minBy { surfaceDistance(from, it) }

    /** Apparent angular radius in degrees (for debug / HUD). */
    fun apparentDiameterDeg(from: Vector3): Float {
        val d = distanceFrom(from).coerceAtLeast(1f)
        return Math.toDegrees(2.0 * kotlin.math.atan((RADIUS / d).toDouble())).toFloat()
    }
}

package com.android.voidrise.game.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3

/**
 * A hand-crafted solar system with 4 massive, dramatically separated planets.
 *
 * Layout from starting position (0, 20, -500) facing -Z:
 *
 *  Kepler  (Ocean, right):   ~2700 units away  – 34° right,  huge blue world
 *  Pyros   (Lava, far left): ~3300 units away  – 40° left,   red volcanic world
 *  Verdana (Jungle, center): ~5800 units away  – 5° right,   giant green world
 *  Glacius (Ice, far right): ~7100 units away  – 35° right,  icy giant
 *  Helios  (Star, behind):   ~3100 units away  – illuminates the scene
 *
 * Planets are separated by 30-60° as seen from the starting position — no
 * clustering. Each world fills a completely different part of the sky.
 */
class Universe {

    data class Planet(
        val name     : String,
        val position : Vector3,
        val radius   : Float,
        /** 0..1 → planet.frag u_seed: floor(seed*5)=arch (0=Ocean 1=Jungle 2=Desert 3=Ice 4=Lava) */
        val seed     : Float,
        /** 2=STAR_REMNANT  3=PLANET */
        val type     : Int,
        val baseColor: Color,
        val glowColor: Color,
    )

    val planets: List<Planet> = listOf(

        // ── Kepler – Ocean World ──────────────────────────────────────────────
        // Right of center, ~34° right from forward, 2700 units → ~39° angular size
        Planet(
            name      = "Kepler",
            position  = Vector3(700f, -130f, -2900f),
            radius    = 820f,
            seed      = 0.04f,   // arch 0 = Ocean
            type      = 3,
            baseColor = Color(0.10f, 0.32f, 0.88f, 1f),
            glowColor = Color(0.38f, 0.68f, 1.00f, 0.8f),
        ),

        // ── Pyros – Lava World ────────────────────────────────────────────────
        // Far left, ~40° left from forward, 3300 units → ~34° angular size
        Planet(
            name      = "Pyros",
            position  = Vector3(-1600f, 140f, -3100f),
            radius    = 740f,
            seed      = 0.88f,   // arch 4 = Lava
            type      = 3,
            baseColor = Color(0.65f, 0.12f, 0.04f, 1f),
            glowColor = Color(1.00f, 0.42f, 0.08f, 0.9f),
        ),

        // ── Verdana – Jungle World ────────────────────────────────────────────
        // Nearly centered, ~5° right from forward, 5800 units → ~21° angular size
        // Huge but far — fills horizon when you approach
        Planet(
            name      = "Verdana",
            position  = Vector3(300f, 330f, -6100f),
            radius    = 1050f,
            seed      = 0.22f,   // arch 1 = Jungle
            type      = 3,
            baseColor = Color(0.14f, 0.52f, 0.20f, 1f),
            glowColor = Color(0.35f, 1.00f, 0.45f, 0.7f),
        ),

        // ── Glacius – Ice World ───────────────────────────────────────────────
        // Far upper-right, ~35° right, 7200 units → ~17° angular size
        Planet(
            name      = "Glacius",
            position  = Vector3(3600f, 480f, -6500f),
            radius    = 980f,
            seed      = 0.64f,   // arch 3 = Ice
            type      = 3,
            baseColor = Color(0.65f, 0.82f, 0.96f, 1f),
            glowColor = Color(0.80f, 0.95f, 1.00f, 0.7f),
        ),

        // ── Helios – Star ─────────────────────────────────────────────────────
        // Behind and above — acts as the system's sun, illuminates the scene.
        Planet(
            name      = "Helios",
            position  = Vector3(2400f, 1100f, 2200f),
            radius    = 520f,
            seed      = 0.50f,
            type      = 2,   // STAR_REMNANT
            baseColor = Color(1.0f, 0.85f, 0.30f, 1f),
            glowColor = Color(1.0f, 0.96f, 0.55f, 1f),
        ),
    )
}

package com.android.voidrise.game.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2

enum class MatterType(val mass: Float, val radius: Float, val color: Color, val glowColor: Color) {
    DUST(
        mass      = 1.5f,
        radius    = 3f,
        color     = Color(0.55f, 0.88f, 1.0f, 0.9f),
        glowColor = Color(0.6f,  1.0f,  1.0f, 0.6f),
    ),
    ASTEROID(
        mass      = 10f,
        radius    = 11f,
        color     = Color(0.70f, 0.55f, 0.38f, 1.0f),
        glowColor = Color(1.0f,  0.72f, 0.38f, 0.5f),
    ),
    STAR_REMNANT(
        mass      = 28f,
        radius    = 18f,
        color     = Color(1.0f,  0.82f, 0.28f, 1.0f),
        glowColor = Color(1.0f,  0.95f, 0.5f,  0.8f),
    ),
    PLANET(
        mass      = 55f,
        radius    = 28f,
        color     = Color(0.3f,  0.72f, 0.45f, 1.0f),
        glowColor = Color(0.4f,  1.0f,  0.6f,  0.6f),
    ),
}

class Matter(
    val position : Vector2 = Vector2(),
    val velocity : Vector2 = Vector2(),
    val type     : MatterType,
    /** Höhen-Offset im 3D-Raum (Y-Achse). */
    val y3D      : Float = 0f,
) {
    var alive  : Boolean = true
    var pullT  : Float   = 0f  // 0..1, wie stark die Gravitation zieht

    val mass   : Float get() = type.mass
    val radius : Float get() = type.radius
    val color  : Color get() = type.color
}

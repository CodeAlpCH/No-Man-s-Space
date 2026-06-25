package com.android.voidrise.game.entities

import com.badlogic.gdx.math.Vector3
import kotlin.math.sqrt

/**
 * The black hole as a world object.
 * Sits at a fixed position and exerts gravity on everything nearby.
 */
class BlackHoleEntity(
    val position: Vector3 = Vector3(0f, 0f, 0f),
) {
    // Visual properties (fed to BlackHoleRenderer)
    val radius      = 40f    // event-horizon radius (bigger = more imposing)
    val diskRadius  = 130f   // accretion disk outer edge

    // Gameplay properties
    val gravRadius  = 600f   // zone where gravity is felt
    val gravStrength = 3500f // tuned for smooth spiral-in

    var spinAngle   = 0f
    var spinSpeed   = 3.0f
    var nitro       = 0f     // 0..1, pulses when ship is close

    fun update(delta: Float, shipDistFraction: Float) {
        // BH spins faster when ship is closer (dramatic effect)
        spinSpeed = 2.5f + shipDistFraction * 8f
        spinAngle += spinSpeed * delta
        nitro = nitro * (1f - delta * 2f) + shipDistFraction * delta * 2f
    }

    /** Gravitational acceleration vector toward the black hole. */
    fun gravityOn(shipPos: Vector3): Vector3 {
        val dir  = Vector3(position).sub(shipPos)
        val dist = dir.len().coerceAtLeast(0.1f)
        // Inverse-square gravity, capped at close range for safety
        val accel = (gravStrength / (dist * dist)).coerceAtMost(120f)
        return dir.nor().scl(accel)
    }

    fun isInGravField(pos: Vector3) = position.dst(pos) < gravRadius
    fun isSwallowed(pos: Vector3)   = position.dst(pos) < radius * 1.05f

    /** 0..1 — how close ship is relative to gravity radius. */
    fun proximityFraction(pos: Vector3): Float {
        val d = position.dst(pos)
        return if (d >= gravRadius) 0f else (1f - d / gravRadius).coerceIn(0f, 1f)
    }
}

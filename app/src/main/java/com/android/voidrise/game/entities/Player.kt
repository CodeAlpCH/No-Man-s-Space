package com.android.voidrise.game.entities

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.sqrt

class Player(
    /** XZ-Koordinaten im Weltraum (Y = Tiefe im 3D-System). */
    var position: Vector2 = Vector2(),
) {
    var mass: Float = 3f
        private set

    /** Absorptions-Burst (0..1). Treibt Spin, Glow und Partikel. */
    var nitro: Float = 0f
        private set

    var spinAngle: Float = 0f
        private set

    var spinSpeed: Float = 3.5f
        private set

    // ── Speed Boost ────────────────────────────────────────────────────────────
    var boostActive: Boolean = false
        private set

    var boostTimer: Float = 0f
        private set

    var boostCooldown: Float = 0f
        private set

    /** Visueller Boost-Glow für Shader (0..1). */
    var boostGlow: Float = 0f
        private set

    // ── Derived stats ──────────────────────────────────────────────────────────
    val radius: Float
        get() = 8f + sqrt(mass) * 2.4f

    val gravityRadius: Float
        get() = radius * 4.5f + sqrt(mass) * 6f

    val moveSpeed: Float
        get() {
            val base = 220f / sqrt(mass / 12f).coerceAtLeast(0.65f)
            return if (boostActive) base * BOOST_SPEED_MUL else base
        }

    val canBoost: Boolean
        get() = boostCooldown <= 0f && mass > MIN_MASS_FOR_BOOST

    // ── Actions ────────────────────────────────────────────────────────────────
    fun activateBoost() {
        if (!canBoost) return
        boostActive   = true
        boostTimer    = BOOST_DURATION
        boostCooldown = BOOST_COOLDOWN
        // Boost costs a little mass (Materie als Triebstrahl ausgestoßen)
        mass = (mass - mass * BOOST_MASS_COST).coerceAtLeast(MIN_MASS_FOR_BOOST)
        nitro = 1f
    }

    fun absorb(matterMass: Float) {
        mass      += matterMass
        nitro      = 1f
        spinSpeed += 6f + matterMass * 0.35f
    }

    fun update(delta: Float, isMoving: Boolean) {
        nitro = (nitro - delta * 1.8f).coerceAtLeast(0f)

        // Boost timer
        if (boostActive) {
            boostTimer -= delta
            if (boostTimer <= 0f) {
                boostActive = false
                boostTimer  = 0f
            }
        }
        if (boostCooldown > 0f) boostCooldown -= delta

        // Boost glow ramp
        val targetBoostGlow = if (boostActive) 1f else 0f
        boostGlow = MathUtils.lerp(boostGlow, targetBoostGlow, delta * 6f)

        // Spin
        val targetSpin = BASE_SPIN +
            (if (isMoving) 2.2f else 0f) +
            nitro * 14f +
            (if (boostActive) 8f else 0f) +
            sqrt(mass) * 0.08f

        spinSpeed = MathUtils.lerp(spinSpeed, targetSpin, delta * 4f)
        spinAngle += spinSpeed * delta * (1f + nitro * 0.6f)
    }

    companion object {
        private const val BASE_SPIN          = 3.5f
        private const val BOOST_SPEED_MUL    = 2.8f
        private const val BOOST_DURATION     = 2.5f
        private const val BOOST_COOLDOWN     = 5.0f
        private const val BOOST_MASS_COST    = 0.04f
        private const val MIN_MASS_FOR_BOOST = 14f
    }
}

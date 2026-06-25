package com.android.voidrise.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.math.sqrt

/**
 * Absorption-Partikel: entstehen bei Kollision, fliegen ins schwarze Loch.
 * Werden in Screen-Space projiziert und als glühende Punkte gezeichnet.
 */
class ParticleSystem {
    private val pool     = ArrayList<Particle>(MAX_PARTICLES)
    private val tmp3D    = Vector3()

    // ─── Public API ────────────────────────────────────────────────────────────

    /** Burst von Partikeln an Weltposition (wx, wy3D, wz) in Richtung Loch (hx,hy3D,hz). */
    fun burst(
        wx: Float, wy3D: Float, wz: Float,
        hx: Float, hy3D: Float, hz: Float,
        color: Color,
        count: Int = 14,
    ) {
        if (pool.size >= MAX_PARTICLES) return
        val dx  = hx - wx
        val dy  = hy3D - wy3D
        val dz  = hz - wz
        val len = sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat().coerceAtLeast(0.01f)

        repeat(count) {
            if (pool.size >= MAX_PARTICLES) return@repeat
            val speed = 80f + MathUtils.random(120f)
            val scatter = 0.35f
            pool.add(Particle(
                wx, wy3D, wz,
                dx / len * speed + MathUtils.random(-scatter, scatter) * speed,
                dy / len * speed + MathUtils.random(-scatter, scatter) * speed,
                dz / len * speed + MathUtils.random(-scatter, scatter) * speed,
                Color(color).also { it.a = 0.7f + MathUtils.random(0.3f) },
                life = 0.5f + MathUtils.random(0.5f),
            ))
        }
    }

    fun update(delta: Float) {
        pool.removeAll { p ->
            p.x    += p.vx * delta
            p.y    += p.vy * delta
            p.z    += p.vz * delta
            p.vx   *= 1.08f // accelerate toward hole
            p.vy   *= 1.08f
            p.vz   *= 1.08f
            p.life -= delta
            p.life <= 0f
        }
    }

    fun draw(shapes: ShapeRenderer, camera: PerspectiveCamera, screenW: Float, screenH: Float) {
        if (pool.isEmpty()) return

        shapes.projectionMatrix.setToOrtho2D(0f, 0f, screenW, screenH)
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        for (p in pool) {
            val t = (p.life / p.maxLife).coerceIn(0f, 1f)
            tmp3D.set(p.x, p.y, p.z)
            camera.project(tmp3D)
            if (tmp3D.z < 0f || tmp3D.z > 1f) continue
            val screenY = tmp3D.y // camera.project() returns Y from bottom
            val radius  = (1.5f + t * 3.5f)
            shapes.color.set(p.color.r, p.color.g, p.color.b, t * p.color.a)
            shapes.circle(tmp3D.x, screenY, radius)
        }

        shapes.end()
    }

    fun clear() = pool.clear()

    // ─── Internals ─────────────────────────────────────────────────────────────

    private data class Particle(
        var x: Float, var y: Float, var z: Float,
        var vx: Float, var vy: Float, var vz: Float,
        val color: Color,
        var life: Float,
    ) {
        val maxLife: Float = life
    }

    companion object {
        private const val MAX_PARTICLES = 600
    }
}

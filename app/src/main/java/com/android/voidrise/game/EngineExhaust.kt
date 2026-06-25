package com.android.voidrise.game

import com.android.voidrise.game.entities.Spaceship
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3

/**
 * Ion-plasma exhaust — hot core, blue flame, orange trail.
 * Replaces the old white engine spheres.
 */
class EngineExhaust {

    private val pool   = ArrayList<Flame>(512)
    private val tmp3D  = Vector3()
    private val back   = Vector3()
    private val spread = Vector3()

    fun update(ship: Spaceship, dt: Float) {
        val thrust = ship.thrustLevel
        if (thrust < 0.02f) {
            fadeAll(dt)
            return
        }

        val (eL, eR) = ship.enginePositions()
        back.set(ship.forward).scl(-1f)

        val spawnRate = thrust * 140f * dt
        var budget = spawnRate.toInt().coerceIn(0, 12)
        if (budget == 0 && MathUtils.random() < spawnRate) budget = 1

        repeat(budget) {
            emit(eL, back, ship.right, ship.up, thrust)
            emit(eR, back, ship.right, ship.up, thrust)
        }

        pool.removeAll { f ->
            f.x += f.vx * dt
            f.y += f.vy * dt
            f.z += f.vz * dt
            f.vx *= 0.92f
            f.vy *= 0.92f
            f.vz *= 0.92f
            f.life -= dt
            f.life <= 0f
        }
    }

    private fun fadeAll(dt: Float) {
        pool.removeAll { f ->
            f.life -= dt * 2.5f
            f.life <= 0f
        }
    }

    private fun emit(
        engine: Vector3,
        back: Vector3,
        right: Vector3,
        up: Vector3,
        thrust: Float,
    ) {
        if (pool.size >= MAX_FLAMES) return

        val speed = 28f + thrust * 55f
        val ox = MathUtils.random(-0.12f, 0.12f)
        val oy = MathUtils.random(-0.10f, 0.10f)

        spread.set(right).scl(ox).add(tmp3D.set(up).scl(oy))

        pool.add(
            Flame(
                x = engine.x + spread.x,
                y = engine.y + spread.y,
                z = engine.z + spread.z,
                vx = back.x * speed + spread.x * 4f + MathUtils.random(-2f, 2f),
                vy = back.y * speed + spread.y * 4f + MathUtils.random(-2f, 2f),
                vz = back.z * speed + spread.z * 4f + MathUtils.random(-2f, 2f),
                size = 0.35f + thrust * 0.55f + MathUtils.random(0.15f),
                life = 0.18f + thrust * 0.28f + MathUtils.random(0.08f),
            ),
        )
    }

    fun draw(shapes: ShapeRenderer, camera: PerspectiveCamera, screenW: Float, screenH: Float) {
        if (pool.isEmpty()) return

        shapes.projectionMatrix.setToOrtho2D(0f, 0f, screenW, screenH)
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        for (f in pool) {
            tmp3D.set(f.x, f.y, f.z)
            camera.project(tmp3D)
            if (tmp3D.z < 0f || tmp3D.z > 1f) continue

            val t = (f.life / f.maxLife).coerceIn(0f, 1f)
            val sx = tmp3D.x
            val sy = tmp3D.y
            val depthScale = (1f - tmp3D.z * 0.35f).coerceIn(0.5f, 1f)
            val baseR = f.size * depthScale * (18f + t * 12f)

            // Outer orange trail
            val trailA = t * 0.35f
            shapes.color.set(1.0f, 0.38f, 0.06f, trailA)
            shapes.circle(sx, sy, baseR * 1.8f)

            // Blue plasma mid
            val midA = t * 0.55f
            shapes.color.set(0.25f, 0.62f, 1.0f, midA)
            shapes.circle(sx, sy, baseR * 1.1f)

            // White-hot core
            shapes.color.set(0.92f, 0.97f, 1.0f, t * 0.85f)
            shapes.circle(sx, sy, baseR * 0.45f)
        }

        shapes.end()
    }

    fun clear() = pool.clear()

    private data class Flame(
        var x: Float, var y: Float, var z: Float,
        var vx: Float, var vy: Float, var vz: Float,
        val size: Float,
        var life: Float,
    ) {
        val maxLife: Float = life
    }

    companion object {
        private const val MAX_FLAMES = 512
    }
}

package com.android.voidrise.game.render

import com.android.voidrise.game.entities.Spaceship
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3

class FlightCamera {

    val cam = PerspectiveCamera(72f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    private val smoothPos = Vector3()
    private val smoothUp  = Vector3(0f, 1f, 0f)
    private val tmpV      = Vector3()

    init {
        cam.near = 0.5f
        cam.far  = 18000f
    }

    /** Call once before first render to avoid lerp pop. */
    fun snapTo(ship: Spaceship) {
        val pos = desiredPos(ship)
        smoothPos.set(pos)
        smoothUp.set(ship.up)
        cam.position.set(pos)
        cam.lookAt(ship.position)
        cam.up.set(ship.up)
        cam.update()
    }

    fun update(ship: Spaceship, delta: Float) {
        val desired = desiredPos(ship)

        // Smooth position follow
        val tPos = (delta * 7f).coerceAtMost(1f)
        smoothPos.lerp(desired, tPos)

        // Smooth up vector follow
        val tUp = (delta * 5f).coerceAtMost(1f)
        smoothUp.lerp(ship.up, tUp).nor()

        cam.position.set(smoothPos)

        // Look a few units ahead of ship so the nose points into view
        tmpV.set(ship.position).mulAdd(ship.forward, 12f)
        cam.lookAt(tmpV)
        cam.up.set(smoothUp)
        cam.update()
    }

    fun resize(w: Int, h: Int) {
        cam.viewportWidth  = w.toFloat()
        cam.viewportHeight = h.toFloat()
        cam.update()
    }

    // Camera sits 14 units behind + 3.5 above the ship
    private fun desiredPos(ship: Spaceship): Vector3 =
        Vector3(ship.position)
            .mulAdd(ship.forward, -14f)
            .mulAdd(ship.up,       3.5f)
}

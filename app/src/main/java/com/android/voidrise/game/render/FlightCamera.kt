package com.android.voidrise.game.render

import com.android.voidrise.game.entities.Spaceship
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3

class FlightCamera {

    val cam = PerspectiveCamera(FOV_BASE, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    private val smoothPos = Vector3()
    private val smoothUp  = Vector3(0f, 1f, 0f)
    private val tmpPos    = Vector3()
    private val tmpV      = Vector3()
    private var smoothZoom = 0f

    init {
        cam.near = 0.5f
        cam.far  = 320_000f
    }

    fun snapTo(ship: Spaceship) {
        smoothZoom = ship.throttle
        val pos = desiredPos(ship, smoothZoom)
        smoothPos.set(pos)
        smoothUp.set(ship.up)
        applyCamera(ship, smoothZoom, pos)
    }

    /**
     * Pull-back / FOV scale with throttle (0 = close, 1 = full boost).
     * During warp: same zoom as full throttle, camera snaps so ship stays in frame.
     */
    fun update(ship: Spaceship, delta: Float, warpEngaged: Boolean = false, warpActive: Boolean = false) {
        val targetZoom = if (warpEngaged) 1f else ship.throttle
        smoothZoom = MathUtils.lerp(smoothZoom, targetZoom, (delta * 5f).coerceAtMost(1f))

        val desired = desiredPos(ship, smoothZoom)
        if (warpActive) {
            smoothPos.set(desired)
            smoothUp.set(ship.up).nor()
        } else {
            val tPos = (delta * 7f).coerceAtMost(1f)
            smoothPos.lerp(desired, tPos)
            val tUp = (delta * 5f).coerceAtMost(1f)
            smoothUp.lerp(ship.up, tUp).nor()
        }

        applyCamera(ship, smoothZoom, smoothPos)
    }

    fun resize(w: Int, h: Int) {
        cam.viewportWidth  = w.toFloat()
        cam.viewportHeight = h.toFloat()
        cam.update()
    }

    private fun applyCamera(ship: Spaceship, zoom: Float, camPos: Vector3) {
        cam.position.set(camPos)
        val look = MathUtils.lerp(LOOK_BASE, LOOK_FULL, zoom)
        cam.fieldOfView = MathUtils.lerp(FOV_BASE, FOV_FULL, zoom)
        tmpV.set(ship.position).mulAdd(ship.forward, look)
        cam.lookAt(tmpV)
        cam.up.set(smoothUp)
        cam.update()
    }

    private fun desiredPos(ship: Spaceship, zoom: Float): Vector3 {
        val back = MathUtils.lerp(BACK_BASE, BACK_FULL, zoom)
        val up   = MathUtils.lerp(UP_BASE, UP_FULL, zoom)
        return tmpPos.set(ship.position).mulAdd(ship.forward, -back).mulAdd(ship.up, up)
    }

    companion object {
        private const val FOV_BASE  = 72f
        private const val FOV_FULL  = 84f
        private const val BACK_BASE   = 14f
        private const val BACK_FULL   = 24f
        private const val UP_BASE     = 3.5f
        private const val UP_FULL     = 5.5f
        private const val LOOK_BASE   = 12f
        private const val LOOK_FULL   = 22f
    }
}

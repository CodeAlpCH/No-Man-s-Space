package com.android.voidrise.game.warp

import com.android.voidrise.game.world.WorldPlanet
import com.badlogic.gdx.math.Vector3

class WarpSystem {

    enum class State { IDLE, COUNTDOWN, ACTIVE }

    var state = State.IDLE
        private set

    var countdownTimer = 0f
        private set
    var countdownDigit = 3
        private set

    var hudMessage: String? = null
        private set
    var messageAlpha = 0f
        private set

    private var messageTimer = 0f

    fun surfaceDistanceKm(from: Vector3): Float = WorldPlanet.surfaceDistance(from)

    fun isActive(): Boolean = state == State.ACTIVE

    fun isCharging(): Boolean = state == State.COUNTDOWN

    fun requestActivation(shipPos: Vector3) {
        if (state == State.ACTIVE || state == State.COUNTDOWN) return

        if (surfaceDistanceKm(shipPos) < WorldPlanet.ATMOSPHERE_SURFACE_KM) {
            showMessage(MSG_TOO_CLOSE)
            return
        }
        state = State.COUNTDOWN
        countdownTimer = COUNTDOWN_SEC
        countdownDigit = 3
    }

    /** Tap WARP again to abort countdown or drop out of hyperdrive. */
    fun cancel() {
        if (state == State.IDLE) return
        state = State.IDLE
        countdownTimer = 0f
    }

    fun isEngaged(): Boolean = state == State.COUNTDOWN || state == State.ACTIVE

    fun update(dt: Float, shipPos: Vector3): Boolean {
        if (messageTimer > 0f) {
            messageTimer -= dt
            messageAlpha = (messageTimer / MESSAGE_DURATION).coerceIn(0f, 1f)
            if (messageTimer <= 0f) {
                hudMessage = null
                messageAlpha = 0f
            }
        }

        when (state) {
            State.IDLE -> return false

            State.COUNTDOWN -> {
                countdownTimer -= dt
                countdownDigit = kotlin.math.ceil(countdownTimer.toDouble()).toInt().coerceIn(1, 3)
                if (countdownTimer <= 0f) {
                    if (surfaceDistanceKm(shipPos) < WorldPlanet.ATMOSPHERE_SURFACE_KM) {
                        showMessage(MSG_TOO_CLOSE)
                        state = State.IDLE
                        return false
                    }
                    state = State.ACTIVE
                    return true
                }
                return false
            }

            State.ACTIVE -> {
                if (surfaceDistanceKm(shipPos) < WorldPlanet.ATMOSPHERE_SURFACE_KM) {
                    deactivate()
                    return false
                }
                return true
            }
        }
    }

    fun deactivate() {
        state = State.IDLE
        countdownTimer = 0f
    }

    private fun showMessage(text: String) {
        hudMessage = text
        messageTimer = MESSAGE_DURATION
        messageAlpha = 1f
    }

    companion object {
        const val WARP_SPEED_KM_S = 2500f
        const val MIN_SURFACE_KM    = WorldPlanet.ATMOSPHERE_SURFACE_KM
        const val COUNTDOWN_SEC     = 3f
        private const val MESSAGE_DURATION = 3.2f
        const val MSG_TOO_CLOSE = "ZU NAH AN PLANETENATMOSPHÄRE"
    }
}

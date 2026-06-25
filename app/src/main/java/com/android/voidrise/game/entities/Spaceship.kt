package com.android.voidrise.game.entities

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3

class Spaceship {

    // ─── State ────────────────────────────────────────────────────────────────
    val position   = Vector3(0f, 8f, -120f)
    val velocity   = Vector3()
    val quaternion = Quaternion()

    // Derived direction vectors (updated every frame from quaternion)
    val forward = Vector3(0f, 0f, -1f)
    val up      = Vector3(0f, 1f, 0f)
    val right   = Vector3(1f, 0f, 0f)

    // ─── Input (set each frame by GameScreen) ─────────────────────────────────
    var pitchInput  = 0f    // -1 (nose down) .. +1 (nose up)
    var yawInput    = 0f    // -1 (left)      .. +1 (right)
    /** 0..1 from the throttle lever – 0 means engines off, 1 = full thrust */
    var throttle    = 0f
    /** Boost button: extra burst on top of throttle */
    var boostActive = false

    /** 0..1 visual only – drives engine glow size */
    var thrustLevel = 0f
        private set

    // ─── Internal ─────────────────────────────────────────────────────────────
    private val rotMat = Matrix4()
    private val tmpQ   = Quaternion()

    // ─── Update ───────────────────────────────────────────────────────────────

    fun update(delta: Float) {
        // ── Rotation (right joystick) ────────────────────────────────────────
        val pitchDeg = pitchInput * TURN_RATE * delta * MathUtils.radiansToDegrees
        val yawDeg   = yawInput   * TURN_RATE * delta * MathUtils.radiansToDegrees

        if (yawDeg != 0f) {
            tmpQ.setFromAxis(up, -yawDeg)
            quaternion.mulLeft(tmpQ)
        }
        if (pitchDeg != 0f) {
            tmpQ.setFromAxis(right, pitchDeg)
            quaternion.mulLeft(tmpQ)
        }
        quaternion.nor()

        // Rebuild local axes from quaternion
        rotMat.set(quaternion)
        forward.set(0f, 0f, -1f).mul(rotMat)
        up.set(0f, 1f,  0f).mul(rotMat)
        right.set(1f, 0f, 0f).mul(rotMat)

        // ── Thrust (throttle lever + optional boost) ─────────────────────────
        val normalThrust  = throttle * CRUISE_THRUST
        val boostThrust   = if (boostActive) BOOST_EXTRA else 0f
        val totalThrust   = normalThrust + boostThrust

        velocity.mulAdd(forward, totalThrust * delta)

        // ── Speed cap ────────────────────────────────────────────────────────
        val maxV = if (boostActive) MAX_BOOST_SPEED
                   else throttle * MAX_CRUISE_SPEED + 1f   // always at least 1 to prevent NaN
        val spd = velocity.len()
        if (spd > maxV) velocity.scl(maxV / spd)

        // ── Frame-rate independent drag ───────────────────────────────────────
        // When throttle = 0 → stronger drag so ship glides to a stop
        // When throttle > 0 → lighter drag (momentum preserved)
        val dragPerSec = if (throttle < 0.02f) DRAG_STOP else DRAG_CRUISE
        val dragFactor = (1f - dragPerSec * delta).coerceAtLeast(0f)
        velocity.scl(dragFactor)

        // ── Integrate ─────────────────────────────────────────────────────────
        position.mulAdd(velocity, delta)

        // ── Visual thrust level (drives engine glow) ─────────────────────────
        thrustLevel = throttle + (if (boostActive) 0.5f else 0f)
    }

    /** World-space positions of the two engine exhausts. */
    fun enginePositions(): Pair<Vector3, Vector3> {
        val tail = Vector3(position).mulAdd(forward, -2.6f).mulAdd(up, -0.08f)
        return Vector3(tail).mulAdd(right, -0.55f) to
               Vector3(tail).mulAdd(right,  0.55f)
    }

    companion object {
        const val CRUISE_THRUST    = 55f      // force per unit throttle
        const val BOOST_EXTRA      = 130f     // extra when boost held
        const val MAX_CRUISE_SPEED = 120f
        const val MAX_BOOST_SPEED  = 280f
        const val TURN_RATE        = 1.35f    // rad/s
        const val DRAG_CRUISE      = 0.30f    // velocity loss per second (throttle > 0)
        const val DRAG_STOP        = 1.40f    // velocity loss per second (throttle = 0, stops in ~2s)
    }
}

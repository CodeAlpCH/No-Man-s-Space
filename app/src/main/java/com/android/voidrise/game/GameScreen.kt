package com.android.voidrise.game

import com.android.voidrise.VoidriseGame
import com.android.voidrise.game.entities.BlackHoleEntity
import com.android.voidrise.game.entities.Spaceship
import com.android.voidrise.game.input.ThrottleLever
import com.android.voidrise.game.input.VirtualJoystick
import com.android.voidrise.game.render.BlackHoleRenderer
import com.android.voidrise.game.render.FlightCamera
import com.android.voidrise.game.render.GalaxyRenderer
import com.android.voidrise.game.render.SpaceshipRenderer
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

class GameScreen(private val game: VoidriseGame) : ScreenAdapter() {

    // ─── Core Systems ─────────────────────────────────────────────────────────
    private val ship           = Spaceship()
    private val flightCam      = FlightCamera()
    private val bhEntity       = BlackHoleEntity(Vector3(0f, 0f, 0f))
    private val bhRenderer     = BlackHoleRenderer()
    private val shipRenderer   = SpaceshipRenderer()
    private val galaxyRenderer = GalaxyRenderer()
    private val particles      = ParticleSystem()
    private val audio          = AudioManager()

    // ─── Controls ─────────────────────────────────────────────────────────────
    private val joystick = VirtualJoystick()    // right side – heading
    private val throttle = ThrottleLever()      // left side  – speed

    // Boost: center button between throttle and joystick
    private var boostCx    = 0f
    private var boostCy    = 0f
    private val boostR     = 58f
    private var boostActive = false

    // ─── HUD State ────────────────────────────────────────────────────────────
    private var proximityAlpha = 0f
    private var displaySpeed   = 0f
    private var time           = 0f

    // ─── Init ─────────────────────────────────────────────────────────────────

    override fun show() {
        ship.position.set(0f, 30f, -380f)

        bhRenderer.init()
        shipRenderer.init()
        galaxyRenderer.init()
        audio.init()

        flightCam.snapTo(ship)
        layoutControls()
    }

    private fun layoutControls() {
        val w = Gdx.graphics.width
        val h = Gdx.graphics.height
        joystick.layout(w, h)
        throttle.layout(w, h)
        // Boost button: horizontally centered, above the bottom edge
        boostCx = w * 0.50f
        boostCy = boostR + 36f
    }

    // ─── Render Loop ──────────────────────────────────────────────────────────

    override fun render(delta: Float) {
        val dt = delta.coerceAtMost(0.05f)
        time += dt

        joystick.update(dt)
        throttle.update(dt)
        checkBoostButton()
        updateShip(dt)
        updateBH(dt)
        flightCam.update(ship, dt)
        particles.update(dt)

        // ── Draw ──────────────────────────────────────────────────────────────
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Galaxy skybox first — no depth test/write
        galaxyRenderer.render(flightCam.cam)

        // 3D world
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL)
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)

        bhRenderer.renderBH(flightCam.cam, bhEntity, time)

        // Spaceship (ModelBatch manages its own GL state)
        shipRenderer.render(ship, flightCam.cam)
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)

        drawEngineGlow()

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)

        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        particles.draw(game.shapes, flightCam.cam, sw, sh)

        drawHud(sw, sh)
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    private fun updateShip(dt: Float) {
        val dir = joystick.direction()
        ship.pitchInput  = -dir.y
        ship.yawInput    =  dir.x
        ship.throttle    = throttle.value
        ship.boostActive = boostActive

        ship.update(dt)

        // BH gravity
        val prox = bhEntity.proximityFraction(ship.position)
        if (prox > 0f) {
            ship.velocity.mulAdd(bhEntity.gravityOn(ship.position), dt)
            proximityAlpha = MathUtils.lerp(proximityAlpha, prox, dt * 3f)
        } else {
            proximityAlpha = MathUtils.lerp(proximityAlpha, 0f, dt * 2f)
        }

        if (bhEntity.isSwallowed(ship.position)) respawn()

        displaySpeed = MathUtils.lerp(displaySpeed, ship.velocity.len(), dt * 4f)
    }

    private fun updateBH(dt: Float) {
        val prox = bhEntity.proximityFraction(ship.position)
        bhEntity.update(dt, prox)
    }

    private fun respawn() {
        ship.position.set(0f, 30f, -380f)
        ship.velocity.setZero()
        ship.quaternion.idt()
        ship.forward.set(0f, 0f, -1f)
        ship.up.set(0f, 1f, 0f)
        ship.right.set(1f, 0f, 0f)
        flightCam.snapTo(ship)
        audio.playBoost()
    }

    // ─── Draw ─────────────────────────────────────────────────────────────────

    private fun drawEngineGlow() {
        val (eL, eR) = ship.enginePositions()
        val gSize    = 0.5f + ship.thrustLevel * 0.65f
        val eColor   = if (boostActive)
            Color(0.55f, 0.95f, 1.0f, 0.95f)
        else
            Color(0.28f, 0.68f, 1.0f, 0.85f)
        val eGlow    = Color(0.45f, 0.88f, 1.0f, 1.0f)

        bhRenderer.beginPlanetBatch(flightCam.cam)
        bhRenderer.renderPlanetSphere(eL.x, eL.y, eL.z, gSize, eColor, eGlow, 0, time, 0f)
        bhRenderer.renderPlanetSphere(eR.x, eR.y, eR.z, gSize, eColor, eGlow, 0, time, 0f)
        bhRenderer.endPlanetBatch()
    }

    // ─── HUD ──────────────────────────────────────────────────────────────────

    private fun drawHud(sw: Float, sh: Float) {
        game.shapes.projectionMatrix.setToOrtho2D(0f, 0f, sw, sh)
        game.shapes.begin(ShapeRenderer.ShapeType.Filled)

        throttle.draw(game.shapes)
        joystick.draw(game.shapes)
        drawBoostButton()
        drawSpeedBar(sw, sh)
        drawBHProximityWarning(sw, sh)

        game.shapes.end()
    }

    private fun drawBoostButton() {
        // Color: active = hot cyan, idle = dim blue
        val c = if (boostActive)
            Color(0.25f, 1.0f, 0.85f, 0.80f)
        else
            Color(0.20f, 0.65f, 1.0f, 0.42f)

        // Outer ring
        game.shapes.color.set(c)
        game.shapes.circle(boostCx, boostCy, boostR)
        // Fill
        game.shapes.color.set(c.r, c.g, c.b, if (boostActive) 0.25f else 0.08f)
        game.shapes.circle(boostCx, boostCy, boostR - 6f)
        // Center pip
        game.shapes.color.set(0.55f, 0.95f, 1.0f, if (boostActive) 1.0f else 0.40f)
        game.shapes.circle(boostCx, boostCy, boostR * 0.22f)
    }

    private fun drawSpeedBar(sw: Float, sh: Float) {
        val barW = sw * 0.18f
        val barH = 5f
        val bx   = sw * 0.50f - barW * 0.5f   // centered above boost button
        val by   = sh - 36f

        game.shapes.color.set(1f, 1f, 1f, 0.06f)
        game.shapes.rect(bx, by, barW, barH)

        val fill = (displaySpeed / Spaceship.MAX_BOOST_SPEED).coerceIn(0f, 1f)
        val cr   = MathUtils.lerp(0.3f, 1.0f, fill)
        val cg   = MathUtils.lerp(0.8f, 0.35f, fill)
        game.shapes.color.set(cr, cg, 1f - fill * 0.4f, 0.80f)
        game.shapes.rect(bx, by, barW * fill, barH)
    }

    private fun drawBHProximityWarning(sw: Float, sh: Float) {
        if (proximityAlpha < 0.08f) return
        val pulse = MathUtils.sin(time * 4.5f) * 0.35f + 0.65f
        val a     = proximityAlpha * pulse * 0.65f
        game.shapes.color.set(1f, 0.22f, 0.06f, a)
        val b = 7f
        game.shapes.rect(0f, 0f, sw, b)
        game.shapes.rect(0f, sh - b, sw, b)
        game.shapes.rect(0f, 0f, b, sh)
        game.shapes.rect(sw - b, 0f, b, sh)
    }

    // ─── Input ────────────────────────────────────────────────────────────────

    /** Boost: center button between throttle and joystick. */
    private fun checkBoostButton() {
        boostActive = false
        for (i in 0 until minOf(Gdx.input.maxPointers, 5)) {
            if (!Gdx.input.isTouched(i)) continue
            val tx = Gdx.input.getX(i).toFloat()
            val ty = Gdx.graphics.height - Gdx.input.getY(i).toFloat()
            if (Vector2(tx - boostCx, ty - boostCy).len() <= boostR * 1.5f) {
                boostActive = true
                break
            }
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun resize(width: Int, height: Int) {
        flightCam.resize(width, height)
        layoutControls()
    }

    override fun dispose() {
        bhRenderer.dispose()
        shipRenderer.dispose()
        galaxyRenderer.dispose()
        audio.dispose()
        particles.clear()
    }
}

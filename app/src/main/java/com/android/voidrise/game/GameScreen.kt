package com.android.voidrise.game

import com.android.voidrise.VoidriseGame
import com.android.voidrise.game.entities.BlackHoleEntity
import com.android.voidrise.game.entities.Spaceship
import com.android.voidrise.game.input.ThrottleLever
import com.android.voidrise.game.input.VirtualJoystick
import com.android.voidrise.game.render.BlackHoleRenderer
import com.android.voidrise.game.render.ExhaustRenderer
import com.android.voidrise.game.render.FlightCamera
import com.android.voidrise.game.render.GalaxyRenderer
import com.android.voidrise.game.render.PlanetRenderer
import com.android.voidrise.game.render.SpaceshipRenderer
import com.android.voidrise.game.ui.GalaxyMapHud
import com.android.voidrise.game.ui.PlanetDistanceHud
import com.android.voidrise.game.ui.SpeedHud
import com.android.voidrise.game.ui.WarpButton
import com.android.voidrise.game.ui.WarpScreenFx
import com.android.voidrise.game.warp.WarpSystem
import com.android.voidrise.game.world.WorldPlanet
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3

class GameScreen(private val game: VoidriseGame) : ScreenAdapter() {

    companion object {
        /** Temporarily disabled — planet focus next. */
        private const val BLACK_HOLE_ENABLED = false
    }

    // ─── Core Systems ─────────────────────────────────────────────────────────
    private val ship           = Spaceship()
    private val flightCam      = FlightCamera()
    private val bhEntity       = BlackHoleEntity(Vector3(0f, 0f, 0f))
    private val bhRenderer     = BlackHoleRenderer()
    private val planetRenderer = PlanetRenderer(bhRenderer)
    private val shipRenderer   = SpaceshipRenderer()
    private val galaxyRenderer = GalaxyRenderer()
    private val particles      = ParticleSystem()
    private val exhaustRenderer = ExhaustRenderer()
    private val audio          = AudioManager()

    // ─── Controls ─────────────────────────────────────────────────────────────
    private val joystick   = VirtualJoystick()
    private val throttle   = ThrottleLever()
    private val warpButton = WarpButton()
    private val warpSystem = WarpSystem()

    // ─── HUD State ────────────────────────────────────────────────────────────
    private var proximityAlpha = 0f
    private var displaySpeed   = 0f
    private var time           = 0f
    private var planetLabel: PlanetDistanceHud.Label? = null

    // ─── Init ─────────────────────────────────────────────────────────────────

    override fun show() {
        GraphicsQuality.detect()
        ship.position.set(0f, 30f, -380f)

        bhRenderer.init()
        planetRenderer.init()
        shipRenderer.init()
        galaxyRenderer.init()
        exhaustRenderer.init()
        audio.init()

        flightCam.snapTo(ship)
        layoutControls()
    }

    private fun layoutControls() {
        val w = Gdx.graphics.width
        val h = Gdx.graphics.height
        joystick.layout(w, h)
        throttle.layout(w, h)
        warpButton.layout(w, h)
        SpeedHud.layout(w, h, throttle.leverCenterX, throttle.leverBottomY)
    }

    // ─── Render Loop ──────────────────────────────────────────────────────────

    override fun render(delta: Float) {
        val dt = delta.coerceAtMost(0.05f)
        time += dt

        joystick.update(dt)
        throttle.enabled = !warpSystem.isEngaged()
        throttle.update(dt)
        warpButton.update(warpSystem, ship.position)
        updateShip(dt)
        if (BLACK_HOLE_ENABLED) updateBH(dt)
        flightCam.update(ship, dt, warpSystem.isEngaged(), warpSystem.isActive())
        particles.update(dt)

        // ── Draw ──────────────────────────────────────────────────────────────
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Galaxy skybox first — no depth test/write
        if (BLACK_HOLE_ENABLED) {
            galaxyRenderer.render(flightCam.cam, bhEntity.position, bhEntity.radius)
        } else {
            galaxyRenderer.render(flightCam.cam, Vector3(0f, 0f, 1e6f), 0f)
        }

        // 3D world
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL)
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)

        if (BLACK_HOLE_ENABLED) {
            bhRenderer.renderBH(flightCam.cam, bhEntity, time)
        }

        drawWorldPlanet()

        // Spaceship (ModelBatch manages its own GL state)
        shipRenderer.render(ship, flightCam.cam)
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)

        exhaustRenderer.render(ship, flightCam.cam, time)

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)

        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        particles.draw(game.shapes, flightCam.cam, sw, sh)

        drawHud(sw, sh)
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    private var wasWarpActive = false

    private fun updateShip(dt: Float) {
        val dir = joystick.direction()
        ship.pitchInput = -dir.y
        ship.yawInput   =  dir.x
        ship.throttle   = if (warpSystem.isActive()) 1f else throttle.value

        warpSystem.update(dt, ship.position)

        if (wasWarpActive && !warpSystem.isActive()) {
            ship.velocity.set(ship.forward).scl(Spaceship.MAX_CRUISE_SPEED * 0.65f)
        }
        wasWarpActive = warpSystem.isActive()
        ship.warpActive = wasWarpActive

        ship.update(dt)

        if (BLACK_HOLE_ENABLED) {
            val prox = bhEntity.proximityFraction(ship.position)
            if (prox > 0f) {
                ship.velocity.mulAdd(bhEntity.gravityOn(ship.position), dt)
                proximityAlpha = MathUtils.lerp(proximityAlpha, prox, dt * 3f)
            } else {
                proximityAlpha = MathUtils.lerp(proximityAlpha, 0f, dt * 2f)
            }
            if (bhEntity.isSwallowed(ship.position)) respawn()
        }

        displaySpeed = MathUtils.lerp(displaySpeed, ship.velocity.len(), dt * 4f)
    }

    private fun updateBH(dt: Float) {
        val prox = bhEntity.proximityFraction(ship.position)
        bhEntity.update(dt, prox)
    }

    private fun respawn() {
        ship.position.set(0f, 30f, -380f)
        ship.velocity.setZero()
        ship.warpActive = false
        warpSystem.deactivate()
        ship.quaternion.idt()
        ship.forward.set(0f, 0f, -1f)
        ship.up.set(0f, 1f, 0f)
        ship.right.set(1f, 0f, 0f)
        flightCam.snapTo(ship)
        audio.playBoost()
    }

    // ─── Draw ─────────────────────────────────────────────────────────────────

    private fun drawWorldPlanet() {
        planetRenderer.render(flightCam.cam, ship.position, time)
    }

    // ─── HUD ──────────────────────────────────────────────────────────────────

    private fun drawHud(sw: Float, sh: Float) {
        planetLabel = PlanetDistanceHud.compute(
            flightCam.cam, ship.position,
            WorldPlanet.position, WorldPlanet.RADIUS, WorldPlanet.NAME,
            game.hudFont, game.hudFontLarge, sw, sh,
        )

        game.shapes.projectionMatrix.setToOrtho2D(0f, 0f, sw, sh)
        game.shapes.begin(ShapeRenderer.ShapeType.Filled)

        throttle.draw(game.shapes)
        joystick.draw(game.shapes)
        SpeedHud.drawBackground(game.shapes)
        warpButton.drawFilled(game.shapes, warpSystem, time)
        SpeedHud.drawBar(game.shapes, displaySpeed, warpSystem.isActive())
        drawBHProximityWarning(sw, sh)
        GalaxyMapHud.draw(
            game.shapes, sw, sh,
            ship.position, ship.right, ship.forward,
            WorldPlanet.position, time,
            linePass = false,
        )
        planetLabel?.let {
            PlanetDistanceHud.drawBackground(game.shapes, it, game.hudFont, game.hudFontLarge)
        }

        game.shapes.end()

        game.shapes.begin(ShapeRenderer.ShapeType.Line)
        GalaxyMapHud.draw(
            game.shapes, sw, sh,
            ship.position, ship.right, ship.forward,
            WorldPlanet.position, time,
            linePass = true,
        )
        game.shapes.end()

        game.batch.projectionMatrix.setToOrtho2D(0f, 0f, sw, sh)
        game.batch.begin()
        planetLabel?.let { label ->
            PlanetDistanceHud.drawText(game.batch, label, game.hudFont, game.hudFontLarge)
        }
        SpeedHud.drawText(game.batch, game.hudFont, game.hudFontLarge, displaySpeed, warpSystem.isActive())
        warpButton.drawLabel(game.batch, game.hudFont, warpSystem)
        WarpScreenFx.drawOverlayText(game.batch, game.hudFont, sw, sh, warpSystem)
        game.batch.end()
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

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun resize(width: Int, height: Int) {
        flightCam.resize(width, height)
        layoutControls()
    }

    override fun dispose() {
        bhRenderer.dispose()
        planetRenderer.dispose()
        shipRenderer.dispose()
        galaxyRenderer.dispose()
        exhaustRenderer.dispose()
        audio.dispose()
        particles.clear()
    }
}

package com.android.voidrise.game

import com.android.voidrise.VoidriseGame
import com.android.voidrise.game.entities.BlackHoleEntity
import com.android.voidrise.game.entities.Spaceship
import com.android.voidrise.game.input.ThrottleLever
import com.android.voidrise.game.input.VirtualJoystick
import com.android.voidrise.game.render.BlackHoleRenderer
import com.android.voidrise.game.render.AtmosphereFogRenderer
import com.android.voidrise.game.render.ExhaustRenderer
import com.android.voidrise.game.render.FlightCamera
import com.android.voidrise.game.render.GalaxyRenderer
import com.android.voidrise.game.render.PlanetRenderer
import com.android.voidrise.game.render.SpaceshipRenderer
import com.android.voidrise.game.ui.FlightBannerHud
import com.android.voidrise.game.ui.GalaxyMapHud
import com.android.voidrise.game.ui.PlanetDistanceHud
import com.android.voidrise.game.ui.SpeedHud
import com.android.voidrise.game.ui.WarpButton
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
        private const val START_SURFACE_DISTANCE_KM = 10_000f
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
    private val atmosphereFogRenderer = AtmosphereFogRenderer()
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
    private val flightBanner = FlightBannerHud()
    private var wasInsideAtmosphere = false
    private val spawnDir = Vector3()

    // ─── Init ─────────────────────────────────────────────────────────────────

    override fun show() {
        GraphicsQuality.detect()
        placeShipAtPlanetApproach(WorldPlanet.terraNova)

        bhRenderer.init()
        planetRenderer.init()
        shipRenderer.init()
        galaxyRenderer.init()
        exhaustRenderer.init()
        atmosphereFogRenderer.init()
        audio.init()
        warpButton.load()

        flightCam.snapTo(ship)
        layoutControls()
        atmosphereFogRenderer.resize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        wasInsideAtmosphere = isInsideAtmosphere()
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
        warpButton.updateAnim(dt, warpSystem)
        updateShip(dt)
        updateAtmosphereEntry(dt)
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
        atmosphereFogRenderer.render(WorldPlanet.surfaceDistance(ship.position), sw, sh, time)

        drawHud(sw, sh)
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    private var wasWarpActive = false
    private var wasWarpCountdown = false

    private fun updateShip(dt: Float) {
        val dir = joystick.direction()
        ship.pitchInput = -dir.y
        ship.yawInput   =  dir.x
        ship.throttle   = if (warpSystem.isActive()) 1f else throttle.value

        val timerBefore = warpSystem.countdownTimer

        warpSystem.update(dt, ship.position)

        val countdown = warpSystem.isCharging()
        if (countdown) {
            // wasWarpCountdown — not isCharging() before update: button sets COUNTDOWN earlier in the frame
            if (!wasWarpCountdown) audio.playWarpCharge()
            audio.updateWarpCharge(warpSystem.countdownTimer, true)
        } else if (wasWarpCountdown) {
            audio.updateWarpCharge(timerBefore.coerceAtLeast(0f), true)
            audio.stopWarpCharge()
        }
        wasWarpCountdown = countdown

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

        audio.updateEngine(
            throttle = if (warpSystem.isActive()) 1f else throttle.value,
            warpEngaged = warpSystem.isEngaged(),
            dt = dt,
        )
    }

    private fun updateBH(dt: Float) {
        val prox = bhEntity.proximityFraction(ship.position)
        bhEntity.update(dt, prox)
    }

    private fun updateAtmosphereEntry(dt: Float) {
        flightBanner.update(dt)
        val insideAtmosphere = isInsideAtmosphere()
        if (insideAtmosphere && !wasInsideAtmosphere) {
            flightBanner.showAtmosphereEntry()
        }
        wasInsideAtmosphere = insideAtmosphere
    }

    private fun isInsideAtmosphere(): Boolean =
        WorldPlanet.surfaceDistance(ship.position) <= WorldPlanet.ATMOSPHERE_SURFACE_KM

    private fun respawn() {
        placeShipAtPlanetApproach(WorldPlanet.terraNova)
        flightCam.snapTo(ship)
        audio.playBoost()
    }

    private fun placeShipAtPlanetApproach(planet: WorldPlanet.Planet) {
        spawnDir.set(0f, 30f, -380f).sub(planet.position).nor()
        ship.position.set(planet.position).mulAdd(
            spawnDir,
            planet.radius + START_SURFACE_DISTANCE_KM,
        )
        ship.velocity.setZero()
        ship.warpActive = false
        warpSystem.deactivate()
        ship.quaternion.idt()
        ship.forward.set(0f, 0f, -1f)
        ship.up.set(0f, 1f, 0f)
        ship.right.set(1f, 0f, 0f)
    }

    // ─── Draw ─────────────────────────────────────────────────────────────────

    private fun drawWorldPlanet() {
        planetRenderer.render(flightCam.cam, ship.position, time)
    }

    // ─── HUD ──────────────────────────────────────────────────────────────────

    private fun drawHud(sw: Float, sh: Float) {
        planetLabel = WorldPlanet.planets
            .mapNotNull { planet ->
                PlanetDistanceHud.compute(
                    flightCam.cam, ship.position,
                    planet.position, planet.radius, planet.name,
                    game.hudFont, game.hudFontLarge, sw, sh,
                )
            }
            .minByOrNull { it.distKm }

        game.shapes.projectionMatrix.setToOrtho2D(0f, 0f, sw, sh)
        game.shapes.begin(ShapeRenderer.ShapeType.Filled)

        throttle.draw(game.shapes)
        joystick.draw(game.shapes)
        warpButton.drawFilled(game.shapes, warpSystem, time)
        drawBHProximityWarning(sw, sh)
        flightBanner.drawBackground(game.shapes, sw, sh)
        GalaxyMapHud.draw(
            game.shapes, sw, sh,
            ship.position, ship.right, ship.forward,
            WorldPlanet.planets.map { it.position }, time,
            linePass = false,
        )
        planetLabel?.let {
            PlanetDistanceHud.drawBackground(game.shapes, it, game.hudFont, game.hudFontLarge)
        }

        game.shapes.end()

        game.batch.projectionMatrix.setToOrtho2D(0f, 0f, sw, sh)
        game.batch.begin()
        warpButton.drawSprite(game.batch, warpSystem)
        game.batch.end()

        game.shapes.begin(ShapeRenderer.ShapeType.Line)
        warpButton.drawLines(game.shapes, warpSystem, time)
        GalaxyMapHud.draw(
            game.shapes, sw, sh,
            ship.position, ship.right, ship.forward,
            WorldPlanet.planets.map { it.position }, time,
            linePass = true,
        )
        game.shapes.end()

        game.batch.projectionMatrix.setToOrtho2D(0f, 0f, sw, sh)
        game.batch.begin()
        planetLabel?.let { label ->
            PlanetDistanceHud.drawText(game.batch, label, game.hudFont, game.hudFontLarge)
        }
        SpeedHud.drawText(game.batch, game.hudFontSpeed, displaySpeed, warpSystem.isActive())
        flightBanner.drawText(game.batch, game.hudFontLarge, sw, sh)
        warpButton.drawLabel(game.batch, game.hudFontCountdown, game.hudFontWarp, warpSystem)
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
        atmosphereFogRenderer.resize(width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        bhRenderer.dispose()
        planetRenderer.dispose()
        shipRenderer.dispose()
        galaxyRenderer.dispose()
        exhaustRenderer.dispose()
        atmosphereFogRenderer.dispose()
        warpButton.dispose()
        audio.dispose()
        particles.clear()
    }
}

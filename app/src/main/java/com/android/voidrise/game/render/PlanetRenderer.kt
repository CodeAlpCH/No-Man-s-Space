package com.android.voidrise.game.render

import com.android.voidrise.game.GraphicsQuality
import com.android.voidrise.game.world.WorldPlanet
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

/**
 * NMS-style planet LOD:
 * - Far: camera-facing billboard + cheap gradient shader (few GPU ops per pixel)
 * - Near: full 3D sphere + [planet.frag](planet.frag)
 */
class PlanetRenderer(private val sphereRenderer: BlackHoleRenderer) {

    private lateinit var impostorShader: ShaderProgram
    private lateinit var quadMesh: Mesh

    private val mat      = Matrix4()
    private val toCam    = Vector3()
    private val camRight = Vector3()
    private val camUp    = Vector3()
    private val scaleR   = Vector3()
    private val scaleU   = Vector3()
    private val worldPos = Vector3()
    private val renderOrder = mutableListOf<WorldPlanet.Planet>()

    fun init() {
        ShaderProgram.pedantic = false
        val vert = Gdx.files.internal("shaders/planet_impostor.vert").readString()
        val frag = Gdx.files.internal("shaders/planet_impostor.frag").readString()
        impostorShader = ShaderProgram(vert, frag)
        if (!impostorShader.isCompiled) Gdx.app.error("PlanetRenderer", impostorShader.log)
        quadMesh = buildQuadMesh()
    }

    fun render(camera: PerspectiveCamera, shipPos: Vector3, time: Float) {
        renderOrder.clear()
        renderOrder.addAll(WorldPlanet.planets)
        renderOrder.sortByDescending { camera.position.dst2(it.position) }

        renderOrder.forEach { planet ->
            renderPlanet(planet, camera, shipPos, time)
        }
    }

    private fun renderPlanet(
        planet: WorldPlanet.Planet,
        camera: PerspectiveCamera,
        shipPos: Vector3,
        time: Float,
    ) {
        val surfaceDist = WorldPlanet.surfaceDistance(shipPos, planet)
        if (surfaceDist > WorldPlanet.MESH_LOD_SURFACE_KM) {
            renderImpostor(planet, camera)
        } else {
            renderMesh(planet, camera, shipPos, time)
        }
    }

    private fun renderImpostor(planet: WorldPlanet.Planet, camera: PerspectiveCamera) {
        val px = planet.position.x
        val py = planet.position.y
        val pz = planet.position.z
        val d  = planet.radius * 2f

        buildBillboard(px, py, pz, d, camera)

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        Gdx.gl.glDepthMask(true)

        impostorShader.bind()
        impostorShader.setUniformMatrix("u_projViewTrans", camera.combined)
        impostorShader.setUniformMatrix("u_worldTrans", mat)
        impostorShader.setUniformf("u_oceanDeep", planet.baseColor.r * 0.35f, planet.baseColor.g * 0.35f, planet.baseColor.b * 0.45f)
        impostorShader.setUniformf("u_oceanShallow", planet.baseColor.r, planet.baseColor.g, planet.baseColor.b)
        impostorShader.setUniformf("u_atmColor", planet.glowColor.r, planet.glowColor.g, planet.glowColor.b)
        impostorShader.setUniformf("u_seed", planet.seed)

        quadMesh.render(impostorShader, GL20.GL_TRIANGLES)
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun renderMesh(planet: WorldPlanet.Planet, camera: PerspectiveCamera, shipPos: Vector3, time: Float) {
        val surfaceDist = WorldPlanet.surfaceDistance(shipPos, planet)
        sphereRenderer.beginPlanetBatch(camera, GraphicsQuality.planetShaderDetail(shipPos))
        sphereRenderer.renderPlanetSphere(
            planet.position.x, planet.position.y, planet.position.z,
            planet.radius,
            planet.baseColor, planet.glowColor,
            planet.type, time, 0f, planet.seed,
        )
        sphereRenderer.endPlanetBatch()

        val cloudDensity = cloudDensity(surfaceDist)
        if (cloudDensity > 0.01f) {
            sphereRenderer.renderCloudShell(
                camera,
                planet.position.x, planet.position.y, planet.position.z,
                planet.radius * 1.007f,
                time,
                cloudDensity,
            )
        }

        val shellDensity = atmosphereDensity(surfaceDist)
        if (shellDensity > 0.01f) {
            sphereRenderer.renderAtmosphereShell(
                camera,
                planet.position.x, planet.position.y, planet.position.z,
                planet.radius * 1.015f,
                planet.glowColor,
                shellDensity,
            )
        }
    }

    private fun cloudDensity(surfaceDistKm: Float): Float {
        val approachFade = 1f - ((surfaceDistKm - 180f) / (WorldPlanet.MESH_LOD_SURFACE_KM - 180f)).coerceIn(0f, 1f)
        val underCloudFade = ((surfaceDistKm - 90f) / 180f).coerceIn(0f, 1f)
        return (0.22f + approachFade * 0.56f) * underCloudFade * GraphicsQuality.cloudShellScale
    }

    private fun atmosphereDensity(surfaceDistKm: Float): Float {
        val approachFade = 1f - ((surfaceDistKm - 120f) / (WorldPlanet.MESH_LOD_SURFACE_KM - 120f)).coerceIn(0f, 1f)
        val insideFade = ((surfaceDistKm - 520f) / 200f).coerceIn(0f, 1f)
        return (0.16f + approachFade * 0.42f) * insideFade * GraphicsQuality.atmosphereShellScale
    }

    /** Quad faces the camera — always looks like a round planet disc from afar. */
    private fun buildBillboard(wx: Float, wy: Float, wz: Float, diameter: Float, camera: PerspectiveCamera) {
        toCam.set(camera.position.x - wx, camera.position.y - wy, camera.position.z - wz).nor()
        camRight.set(camera.up).crs(toCam).nor()
        camUp.set(toCam).crs(camRight).nor()

        val hs = diameter * 0.5f
        scaleR.set(camRight).scl(hs)
        scaleU.set(camUp).scl(hs)
        worldPos.set(wx, wy, wz)
        mat.set(scaleR, scaleU, toCam, worldPos)
    }

    private fun buildQuadMesh(): Mesh {
        val verts = floatArrayOf(
            -1f, -1f, 0f,  0f, 0f,
             1f, -1f, 0f,  1f, 0f,
            -1f,  1f, 0f,  0f, 1f,
             1f,  1f, 0f,  1f, 1f,
        )
        val indices = shortArrayOf(0, 1, 2, 1, 3, 2)
        val mesh = Mesh(
            true, 4, 6,
            VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
            VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
        )
        mesh.setVertices(verts)
        mesh.setIndices(indices)
        return mesh
    }

    fun dispose() {
        if (::impostorShader.isInitialized) impostorShader.dispose()
        if (::quadMesh.isInitialized) quadMesh.dispose()
    }
}

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

    fun init() {
        ShaderProgram.pedantic = false
        val vert = Gdx.files.internal("shaders/planet_impostor.vert").readString()
        val frag = Gdx.files.internal("shaders/planet_impostor.frag").readString()
        impostorShader = ShaderProgram(vert, frag)
        if (!impostorShader.isCompiled) Gdx.app.error("PlanetRenderer", impostorShader.log)
        quadMesh = buildQuadMesh()
    }

    fun render(camera: PerspectiveCamera, shipPos: Vector3, time: Float) {
        val surfaceDist = WorldPlanet.surfaceDistance(shipPos)
        if (surfaceDist > WorldPlanet.MESH_LOD_SURFACE_KM) {
            renderImpostor(camera)
        } else {
            renderMesh(camera, shipPos, time)
        }
    }

    private fun renderImpostor(camera: PerspectiveCamera) {
        val px = WorldPlanet.position.x
        val py = WorldPlanet.position.y
        val pz = WorldPlanet.position.z
        val d  = WorldPlanet.RADIUS * 2f

        buildBillboard(px, py, pz, d, camera)

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        Gdx.gl.glDepthMask(true)

        impostorShader.bind()
        impostorShader.setUniformMatrix("u_projViewTrans", camera.combined)
        impostorShader.setUniformMatrix("u_worldTrans", mat)
        impostorShader.setUniformf("u_oceanDeep",    0.03f, 0.12f, 0.38f)
        impostorShader.setUniformf("u_oceanShallow", 0.10f, 0.32f, 0.62f)
        impostorShader.setUniformf("u_atmColor",     0.30f, 0.58f, 0.90f)

        quadMesh.render(impostorShader, GL20.GL_TRIANGLES)
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun renderMesh(camera: PerspectiveCamera, shipPos: Vector3, time: Float) {
        val p = WorldPlanet
        sphereRenderer.beginPlanetBatch(camera, GraphicsQuality.planetShaderDetail(shipPos))
        sphereRenderer.renderPlanetSphere(
            p.position.x, p.position.y, p.position.z,
            p.RADIUS,
            p.baseColor, p.glowColor,
            p.TYPE, time, 0f, p.SEED,
        )
        sphereRenderer.endPlanetBatch()
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

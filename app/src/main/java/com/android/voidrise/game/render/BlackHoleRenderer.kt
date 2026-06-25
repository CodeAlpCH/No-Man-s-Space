package com.android.voidrise.game.render

import com.android.voidrise.game.GraphicsQuality
import com.android.voidrise.game.entities.BlackHoleEntity
import com.android.voidrise.game.entities.Player
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

class BlackHoleRenderer {

    // ─── Shaders ──────────────────────────────────────────────────────────────
    private lateinit var bhShader     : ShaderProgram   // event horizon sphere
    private lateinit var diskShader   : ShaderProgram   // accretion disk ring
    private lateinit var glowShader   : ShaderProgram   // halos
    private lateinit var planetShader : ShaderProgram   // matter objects

    // ─── Meshes ───────────────────────────────────────────────────────────────
    private lateinit var sphereMesh : Mesh
    private lateinit var planetMesh   : Mesh   // higher tessellation for giant worlds
    private lateinit var diskMesh   : Mesh

    private val mat  = Matrix4()
    private val mat2 = Matrix4()

    // ─── Init ─────────────────────────────────────────────────────────────────

    fun init() {
        ShaderProgram.pedantic = false
        bhShader     = loadShader("blackhole")
        diskShader   = loadShader("disk")
        glowShader   = loadShader("glow")
        planetShader = loadShader("planet")
        sphereMesh   = buildSphereMesh(36, 36)
        val segs     = GraphicsQuality.planetMeshSegments
        planetMesh   = buildSphereMesh(segs, segs)
        diskMesh     = buildDiskMesh()
    }

    // ─── Black Hole render ────────────────────────────────────────────────────

    /** NMS mode: render BH as a world object at its 3D position. */
    fun renderBH(camera: PerspectiveCamera, bh: BlackHoleEntity, time: Float) {
        val px = bh.position.x
        val py = bh.position.y
        val pz = bh.position.z
        val r  = bh.radius

        Gdx.gl.glEnable(GL20.GL_BLEND)

        // ── Step 1: Event horizon + lensed arcs (opaque, writes depth) ────────
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        Gdx.gl.glDepthMask(true)
        renderEventHorizon3D(camera, px, py, pz, r, time, bh.nitro)

        // ── Step 2: Direct disk crossing (thin band, additive) ────────────────
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        Gdx.gl.glDepthMask(false)
        renderDisk3D(camera, px, py, pz, r, time, bh.nitro, bh.spinAngle)

        Gdx.gl.glDepthMask(true)
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    /** Legacy: render BH at player position (old game mode). */
    fun render(camera: PerspectiveCamera, player: Player, time: Float) {
        val px    = player.position.x
        val pz    = player.position.y
        val r     = player.radius
        val nitro = player.nitro

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glDepthMask(false)

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        renderDisk(camera, px, pz, r, time, nitro, player.spinAngle)

        Gdx.gl.glDepthMask(true)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        renderEventHorizon(camera, px, pz, r, time, nitro)

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun renderEventHorizon3D(
        camera: PerspectiveCamera,
        px: Float, py: Float, pz: Float, r: Float, time: Float, nitro: Float,
    ) {
        mat.setToTranslationAndScaling(px, py, pz, r, r, r)
        bhShader.bind()
        bhShader.setUniformMatrix("u_projViewTrans", camera.combined)
        bhShader.setUniformMatrix("u_worldTrans",    mat)
        bhShader.setUniformf("u_camPos", camera.position.x, camera.position.y, camera.position.z)
        bhShader.setUniformf("u_time",  time)
        bhShader.setUniformf("u_nitro", nitro)
        sphereMesh.render(bhShader, GL20.GL_TRIANGLES)
    }

    private fun renderDisk3D(
        camera: PerspectiveCamera,
        px: Float, py: Float, pz: Float, r: Float,
        time: Float, nitro: Float, @Suppress("UNUSED_PARAMETER") spinAngle: Float,
    ) {
        mat.idt()
        mat.translate(px, py, pz)
        mat.scale(r, r, r)
        // Spin is shader-driven only — no mesh carousel rotation (looks like flat spinning plate)
        mat.rotate(1f, 0f, 0f, 24f)   // steeper tilt → more Interstellar ellipse
        diskShader.bind()
        diskShader.setUniformMatrix("u_projViewTrans", camera.combined)
        diskShader.setUniformMatrix("u_worldTrans",    mat)
        diskShader.setUniformf("u_camPos", camera.position.x, camera.position.y, camera.position.z)
        diskShader.setUniformf("u_bhPos",  px, py, pz)
        diskShader.setUniformf("u_time",    time)
        diskShader.setUniformf("u_nitro",   nitro)
        diskShader.setUniformf("u_opacity", 0.88f + nitro * 0.12f)
        diskMesh.render(diskShader, GL20.GL_TRIANGLES)
    }

    private fun renderEventHorizon(
        camera: PerspectiveCamera,
        px: Float, pz: Float, r: Float, time: Float, nitro: Float,
    ) {
        mat.setToTranslationAndScaling(px, 0f, pz, r, r, r)
        bhShader.bind()
        bhShader.setUniformMatrix("u_projViewTrans", camera.combined)
        bhShader.setUniformMatrix("u_worldTrans",    mat)
        bhShader.setUniformf("u_camPos", camera.position.x, camera.position.y, camera.position.z)
        bhShader.setUniformf("u_time",  time)
        bhShader.setUniformf("u_nitro", nitro)
        sphereMesh.render(bhShader, GL20.GL_TRIANGLES)
    }

    private fun renderDisk(
        camera: PerspectiveCamera,
        px: Float, pz: Float, r: Float, time: Float, nitro: Float, spinAngle: Float,
    ) {
        mat.idt()
        mat.translate(px, 0f, pz)
        mat.scale(r, r, r)
        mat.rotate(0f, 1f, 0f, spinAngle * MathUtils.radiansToDegrees * 0.15f)
        mat.rotate(1f, 0f, 0f, 28f)   // 28° tilt – clearly an ellipse from side-view camera

        diskShader.bind()
        diskShader.setUniformMatrix("u_projViewTrans", camera.combined)
        diskShader.setUniformMatrix("u_worldTrans",    mat)
        diskShader.setUniformf("u_time",    time)
        diskShader.setUniformf("u_nitro",   nitro)
        diskShader.setUniformf("u_opacity", 0.75f + nitro * 0.25f)
        diskMesh.render(diskShader, GL20.GL_TRIANGLES)
    }

    private fun renderHalos(
        camera: PerspectiveCamera,
        px: Float, pz: Float, r: Float, nitro: Float,
    ) {
        glowShader.bind()
        glowShader.setUniformMatrix("u_projViewTrans", camera.combined)
        glowShader.setUniformf("u_camPos", camera.position.x, camera.position.y, camera.position.z)
        glowShader.setUniformf("u_rimPow",  2.0f)
        glowShader.setUniformf("u_coreFill", 0f)
        glowShader.setUniformf("u_pulse",    0f)

        // Only 2 halos – subtle atmospheric corona, not overwhelming rings
        val halos = arrayOf(
            floatArrayOf(3.5f, 0.30f, 0.12f, 0.60f, 0.05f + nitro * 0.05f),
            floatArrayOf(1.8f, 0.48f, 0.22f, 0.85f, 0.10f + nitro * 0.12f),
        )
        for (h in halos) {
            val s = r * h[0]
            mat.setToTranslationAndScaling(px, 0f, pz, s, s, s)
            glowShader.setUniformMatrix("u_worldTrans", mat)
            glowShader.setUniformf("u_color", h[1], h[2], h[3], h[4])
            sphereMesh.render(glowShader, GL20.GL_TRIANGLES)
        }
    }

    // ─── Planet/Matter batch ──────────────────────────────────────────────────

    fun beginPlanetBatch(camera: PerspectiveCamera, shaderDetail: Float) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        Gdx.gl.glDepthMask(true)

        planetShader.bind()
        planetShader.setUniformMatrix("u_projViewTrans", camera.combined)
        planetShader.setUniformf("u_camPos", camera.position.x, camera.position.y, camera.position.z)
        planetShader.setUniformf("u_detail", shaderDetail)
    }

    /**
     * @param type  0=DUST  1=ASTEROID  2=STAR_REMNANT  3=PLANET
     */
    fun renderPlanetSphere(
        wx: Float, wy3D: Float, wz: Float,
        radius: Float,
        baseColor: Color, glowColor: Color,
        type: Int,
        time: Float,
        pullT: Float,
        seed: Float = 0f,
    ) {
        mat.setToTranslationAndScaling(wx, wy3D, wz, radius, radius, radius)
        planetShader.setUniformMatrix("u_worldTrans", mat)
        planetShader.setUniformf("u_baseColor", baseColor.r, baseColor.g, baseColor.b)
        planetShader.setUniformf("u_glowColor", glowColor.r, glowColor.g, glowColor.b)
        planetShader.setUniformf("u_type",  type.toFloat())
        planetShader.setUniformf("u_time",  time)
        planetShader.setUniformf("u_pullT", pullT)
        planetShader.setUniformf("u_seed",  seed)
        val mesh = if (type == 3) planetMesh else sphereMesh
        mesh.render(planetShader, GL20.GL_TRIANGLES)
    }

    fun endPlanetBatch() {
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    // kept for compatibility – unused now
    fun beginMatterBatch(camera: PerspectiveCamera) = beginPlanetBatch(camera, 0f)
    fun endMatterBatch() = endPlanetBatch()

    // ─── Mesh builders ────────────────────────────────────────────────────────

    private fun buildSphereMesh(widthSegs: Int, heightSegs: Int): Mesh {
        val mb = MeshBuilder()
        mb.begin(
            VertexAttributes(
                VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                VertexAttribute(VertexAttributes.Usage.Normal,   3, ShaderProgram.NORMAL_ATTRIBUTE),
                VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2,
                    ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
            ),
            GL20.GL_TRIANGLES,
        )
        SphereShapeBuilder.build(mb, 2f, 2f, 2f, widthSegs, heightSegs)
        return mb.end()
    }

    private fun buildDiskMesh(): Mesh {
        val segs      = 80
        val innerR    = 1.14f   // tighter to event horizon
        val outerR    = 4.60f   // wider — Interstellar-style sweeping ring
        val vertCount = (segs + 1) * 2
        val idxCount  = segs * 6
        val verts     = FloatArray(vertCount * 5)
        val indices   = ShortArray(idxCount)

        for (i in 0..segs) {
            val t = i.toFloat() / segs
            val a = t * MathUtils.PI2
            val c = MathUtils.cos(a); val s = MathUtils.sin(a)
            val iv = i * 2;  val ov = i * 2 + 1

            verts[iv*5+0] = c*innerR; verts[iv*5+1] = 0f; verts[iv*5+2] = s*innerR
            verts[iv*5+3] = t;        verts[iv*5+4] = 0f

            verts[ov*5+0] = c*outerR; verts[ov*5+1] = 0f; verts[ov*5+2] = s*outerR
            verts[ov*5+3] = t;        verts[ov*5+4] = 1f
        }
        for (i in 0 until segs) {
            val v0=(i*2).toShort(); val v1=(i*2+1).toShort()
            val v2=(i*2+2).toShort(); val v3=(i*2+3).toShort()
            val b = i*6
            indices[b]=v0; indices[b+1]=v1; indices[b+2]=v2
            indices[b+3]=v1; indices[b+4]=v3; indices[b+5]=v2
        }
        val mesh = Mesh(
            true, vertCount, idxCount,
            VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
            VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2,
                ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
        )
        mesh.setVertices(verts)
        mesh.setIndices(indices)
        return mesh
    }

    // ─── Shader loader ────────────────────────────────────────────────────────

    private fun loadShader(name: String): ShaderProgram {
        val vert = Gdx.files.internal("shaders/$name.vert").readString()
        val frag = Gdx.files.internal("shaders/$name.frag").readString()
        val sp   = ShaderProgram(vert, frag)
        if (!sp.isCompiled) Gdx.app.error("BHRenderer", "Shader '$name':\n${sp.log}")
        return sp
    }

    fun dispose() {
        bhShader.dispose(); diskShader.dispose()
        glowShader.dispose(); planetShader.dispose()
        sphereMesh.dispose(); planetMesh.dispose(); diskMesh.dispose()
    }
}

package com.android.voidrise.game.render

import com.android.voidrise.game.ui.AtmosphereFog
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4

/** Smooth fullscreen atmosphere haze — no banding grid. */
class AtmosphereFogRenderer {

    private lateinit var shader: ShaderProgram
    private lateinit var quad: Mesh
    private val proj = Matrix4()

    fun init() {
        ShaderProgram.pedantic = false
        val vert = Gdx.files.internal("shaders/atmosphere_fog.vert").readString()
        val frag = Gdx.files.internal("shaders/atmosphere_fog.frag").readString()
        shader = ShaderProgram(vert, frag)
        if (!shader.isCompiled) Gdx.app.error("AtmosphereFogRenderer", shader.log)
        quad = buildQuad()
    }

    fun render(surfaceDistKm: Float, sw: Float, sh: Float) {
        if (surfaceDistKm > AtmosphereFog.FOG_START_KM) return

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glDepthMask(false)

        proj.setToOrtho2D(0f, 0f, sw, sh)
        shader.bind()
        shader.setUniformMatrix("u_projViewTrans", proj)
        shader.setUniformf("u_surfaceDist", surfaceDistKm)
        shader.setUniformf("u_fogStart", AtmosphereFog.FOG_START_KM)

        quad.render(shader, GL20.GL_TRIANGLES)

        Gdx.gl.glDepthMask(true)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
    }

    private fun buildQuad(): Mesh {
        // NDC-style quad mapped to pixel ortho (0,sh)-(sw,0)
        val verts = floatArrayOf(
            0f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
            0f, 0f, 1f, 1f,
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

    fun resize(sw: Float, sh: Float) {
        if (!::quad.isInitialized) return
        val verts = floatArrayOf(
            0f, 0f, 0f, 1f,
            sw, 0f, 1f, 1f,
            0f, sh, 0f, 0f,
            sw, sh, 1f, 0f,
        )
        quad.setVertices(verts)
    }

    fun dispose() {
        if (::shader.isInitialized) shader.dispose()
        if (::quad.isInitialized) quad.dispose()
    }
}

package com.android.voidrise.game.render

import com.android.voidrise.game.entities.Spaceship
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3

/**
 * Two shader plumes — fixed GPU cost (2 draws), looks like real exhaust on all devices.
 */
class ExhaustRenderer {

    private lateinit var shader : ShaderProgram
    private lateinit var quadMesh: Mesh
    private val mat = Matrix4()

    fun init() {
        ShaderProgram.pedantic = false
        val vert = Gdx.files.internal("shaders/exhaust.vert").readString()
        val frag = Gdx.files.internal("shaders/exhaust.frag").readString()
        shader = ShaderProgram(vert, frag)
        if (!shader.isCompiled) Gdx.app.error("ExhaustRenderer", shader.log)
        quadMesh = buildQuadMesh()
    }

    fun render(ship: Spaceship, camera: PerspectiveCamera, time: Float) {
        val thrust = ship.thrustLevel.coerceIn(0f, 1f)
        if (thrust < 0.02f) return

        val (eL, eR) = ship.enginePositions()
        val plumeLen = 1.8f + thrust * 5.2f
        val plumeW   = 0.42f + thrust * 0.38f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        Gdx.gl.glDepthMask(false)

        shader.bind()
        shader.setUniformMatrix("u_projViewTrans", camera.combined)
        shader.setUniformf("u_time", time)
        shader.setUniformf("u_thrust", thrust)

        drawPlume(eL, ship.quaternion, plumeW, plumeLen)
        drawPlume(eR, ship.quaternion, plumeW, plumeLen)

        Gdx.gl.glDepthMask(true)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawPlume(
        engine: Vector3,
        rotation: Quaternion,
        width: Float,
        length: Float,
    ) {
        mat.idt()
        mat.translate(engine)
        mat.rotate(rotation)
        mat.scale(width, 1f, length)
        shader.setUniformMatrix("u_worldTrans", mat)
        quadMesh.render(shader, GL20.GL_TRIANGLES)
    }

  private fun buildQuadMesh(): Mesh {
        // Local +Z = behind ship (exhaust direction)
        val verts = floatArrayOf(
            -0.5f, 0f, 0f,  0f, 0f,
             0.5f, 0f, 0f,  1f, 0f,
            -0.5f, 0f, 1f,  0f, 1f,
             0.5f, 0f, 1f,  1f, 1f,
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
        if (::shader.isInitialized) shader.dispose()
        if (::quadMesh.isInitialized) quadMesh.dispose()
    }
}

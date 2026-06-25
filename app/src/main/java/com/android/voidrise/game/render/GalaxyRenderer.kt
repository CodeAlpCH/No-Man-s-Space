package com.android.voidrise.game.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

/**
 * Renders a full procedural galaxy as a skybox sphere.
 * Always centered at the camera — must be drawn BEFORE any 3D geometry.
 */
class GalaxyRenderer {

    private lateinit var shader  : ShaderProgram
    private lateinit var skyMesh : Mesh
    private val mat = Matrix4()

    fun init() {
        ShaderProgram.pedantic = false
        val vert = Gdx.files.internal("shaders/galaxy.vert").readString()
        val frag = Gdx.files.internal("shaders/galaxy.frag").readString()
        shader = ShaderProgram(vert, frag)
        if (!shader.isCompiled)
            Gdx.app.error("GalaxyRenderer", "Galaxy shader error:\n${shader.log}")

        // Low-poly sphere (we see the inside — no back-face culling needed)
        val mb = MeshBuilder()
        mb.begin(
            VertexAttributes(
                VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE)
            ),
            GL20.GL_TRIANGLES,
        )
        SphereShapeBuilder.build(mb, 2f, 2f, 2f, 28, 28)
        skyMesh = mb.end()
    }

    /**
     * Draw the galaxy — must be called after glClear, before depth-tested geometry.
     * @param bhPos    black hole world position (for gravitational lensing)
     * @param bhRadius black hole visual radius
     */
    fun render(camera: PerspectiveCamera, bhPos: Vector3, bhRadius: Float) {
        // Galaxy is at infinity: no depth test, no depth writes
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glDepthMask(false)
        Gdx.gl.glDisable(GL20.GL_BLEND)
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)   // must see the inside of the sphere

        // Keep sphere centered on camera so it's always "surrounding" us
        mat.setToTranslationAndScaling(
            camera.position.x, camera.position.y, camera.position.z,
            5800f, 5800f, 5800f,
        )

        shader.bind()
        shader.setUniformMatrix("u_projViewTrans", camera.combined)
        shader.setUniformMatrix("u_worldTrans",    mat)
        shader.setUniformf("u_camPos",   camera.position.x, camera.position.y, camera.position.z)
        shader.setUniformf("u_bhPos",  bhPos.x, bhPos.y, bhPos.z)
        shader.setUniformf("u_bhRadius", bhRadius)

        skyMesh.render(shader, GL20.GL_TRIANGLES)

        // Restore state for subsequent passes
        Gdx.gl.glDepthMask(true)
    }

    fun dispose() {
        if (::shader.isInitialized)  shader.dispose()
        if (::skyMesh.isInitialized) skyMesh.dispose()
    }
}

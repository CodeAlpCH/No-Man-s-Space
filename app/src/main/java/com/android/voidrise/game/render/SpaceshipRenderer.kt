package com.android.voidrise.game.render

import com.android.voidrise.game.entities.Spaceship
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder

/**
 * Renders the player's spaceship as a procedural low-poly 3D model.
 * Ship local space: nose = -Z, up = +Y, right = +X.
 */
class SpaceshipRenderer {

    private lateinit var modelBatch : ModelBatch
    private lateinit var model      : Model
    private lateinit var instance   : ModelInstance
    private val environment = Environment()

    fun init() {
        modelBatch = ModelBatch()

        // ── Lighting ──────────────────────────────────────────────────────────
        // Soft deep-space ambient
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.18f, 0.22f, 0.32f, 1f))
        // Primary star (top-left front)
        environment.add(DirectionalLight().set(0.75f, 0.80f, 1.00f, -0.55f, -0.90f, -0.35f))
        // Rim fill from BH side (warm glow)
        environment.add(DirectionalLight().set(0.15f, 0.08f, 0.02f,  0.80f,  0.20f,  0.60f))

        // ── Materials ─────────────────────────────────────────────────────────
        val attrs = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()

        val hullMat = Material(
            ColorAttribute.createDiffuse(Color(0.18f, 0.52f, 0.82f, 1f)),
            ColorAttribute.createEmissive(Color(0.02f, 0.10f, 0.20f, 1f)),
            ColorAttribute.createSpecular(Color(0.55f, 0.75f, 1.00f, 1f)),
            FloatAttribute.createShininess(90f),
        )
        val wingMat = Material(
            ColorAttribute.createDiffuse(Color(0.12f, 0.36f, 0.62f, 1f)),
            ColorAttribute.createEmissive(Color(0.01f, 0.05f, 0.12f, 1f)),
            ColorAttribute.createSpecular(Color(0.40f, 0.60f, 0.90f, 1f)),
            FloatAttribute.createShininess(65f),
        )
        val cockpitMat = Material(
            ColorAttribute.createDiffuse(Color(0.50f, 0.88f, 1.00f, 1f)),
            ColorAttribute.createEmissive(Color(0.08f, 0.24f, 0.38f, 1f)),
            ColorAttribute.createSpecular(Color(0.85f, 0.95f, 1.00f, 1f)),
            FloatAttribute.createShininess(130f),
        )
        val accentMat = Material(
            ColorAttribute.createDiffuse(Color(0.30f, 0.80f, 1.00f, 1f)),
            ColorAttribute.createEmissive(Color(0.06f, 0.28f, 0.45f, 1f)),
            FloatAttribute.createShininess(110f),
        )

        // ── Build model from boxes ────────────────────────────────────────────
        val mb = ModelBuilder()
        mb.begin()

        // Main fuselage (long central hull)
        mb.part("hull", GL20.GL_TRIANGLES, attrs, hullMat)
            .box(0f, 0f, -0.2f, 1.15f, 0.46f, 5.4f)

        // Left swept wing
        mb.part("lwing", GL20.GL_TRIANGLES, attrs, wingMat)
            .box(-2.1f, -0.05f, 0.65f, 2.8f, 0.11f, 2.6f)

        // Right swept wing
        mb.part("rwing", GL20.GL_TRIANGLES, attrs, wingMat)
            .box(2.1f, -0.05f, 0.65f, 2.8f, 0.11f, 2.6f)

        // Cockpit canopy (raised, slightly forward)
        mb.part("cockpit", GL20.GL_TRIANGLES, attrs, cockpitMat)
            .box(0f, 0.36f, -1.4f, 0.58f, 0.20f, 1.1f)

        // Left engine pod (rear)
        mb.part("engL", GL20.GL_TRIANGLES, attrs, accentMat)
            .box(-0.52f, -0.06f, 2.4f, 0.28f, 0.28f, 0.80f)

        // Right engine pod (rear)
        mb.part("engR", GL20.GL_TRIANGLES, attrs, accentMat)
            .box(0.52f, -0.06f, 2.4f, 0.28f, 0.28f, 0.80f)

        // Nose cone accent (thin flat panel on belly)
        mb.part("belly", GL20.GL_TRIANGLES, attrs, accentMat)
            .box(0f, -0.26f, -1.8f, 0.6f, 0.06f, 1.8f)

        model    = mb.end()
        instance = ModelInstance(model)
    }

    fun render(ship: Spaceship, camera: Camera) {
        // Apply ship position + orientation
        instance.transform.set(ship.position, ship.quaternion)

        modelBatch.begin(camera)
        modelBatch.render(instance, environment)
        modelBatch.end()
    }

    fun dispose() {
        if (::modelBatch.isInitialized) modelBatch.dispose()
        if (::model.isInitialized)      model.dispose()
    }
}

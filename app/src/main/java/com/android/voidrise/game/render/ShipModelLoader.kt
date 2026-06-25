package com.android.voidrise.game.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3

/**
 * Loads `assets/models/ship.obj` when present (drop any CC0 OBJ there),
 * otherwise builds a sharper procedural fighter.
 */
object ShipModelLoader {

    private const val SHIP_OBJ = "models/ship.obj"

    fun load(): Model {
        val file = Gdx.files.internal(SHIP_OBJ)
        if (file.exists()) {
            Gdx.app.log("ShipModel", "Loading external model: $SHIP_OBJ")
            return ObjLoader().loadModel(file)
        }
        Gdx.app.log("ShipModel", "No $SHIP_OBJ — using procedural ship")
        return buildProceduralFighter()
    }

    /** Uniform scale + rotation for imported models (nose = -Z). */
    fun importScale(): Vector3 = Vector3(1f, 1f, 1f)

    private fun buildProceduralFighter(): Model {
        val attrs = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()

        val hull = Material(
            ColorAttribute.createDiffuse(Color(0.14f, 0.38f, 0.68f, 1f)),
            ColorAttribute.createEmissive(Color(0.02f, 0.08f, 0.16f, 1f)),
            ColorAttribute.createSpecular(Color(0.50f, 0.70f, 0.95f, 1f)),
            FloatAttribute.createShininess(95f),
        )
        val dark = Material(
            ColorAttribute.createDiffuse(Color(0.08f, 0.22f, 0.42f, 1f)),
            ColorAttribute.createSpecular(Color(0.35f, 0.55f, 0.80f, 1f)),
            FloatAttribute.createShininess(70f),
        )
        val glow = Material(
            ColorAttribute.createDiffuse(Color(0.35f, 0.82f, 1.00f, 1f)),
            ColorAttribute.createEmissive(Color(0.10f, 0.32f, 0.50f, 1f)),
            FloatAttribute.createShininess(120f),
        )

        val mb = ModelBuilder()
        mb.begin()

        // Sleeker fuselage
        mb.part("hull", GL20.GL_TRIANGLES, attrs, hull)
            .box(0f, 0f, -0.4f, 0.95f, 0.38f, 5.8f)

        // Angular nose wedge
        mb.part("nose", GL20.GL_TRIANGLES, attrs, dark)
            .box(0f, -0.05f, -3.2f, 0.55f, 0.22f, 1.6f)

        // Swept wings
        mb.part("lwing", GL20.GL_TRIANGLES, attrs, dark)
            .box(-2.35f, -0.04f, 0.2f, 3.1f, 0.09f, 2.2f)
        mb.part("rwing", GL20.GL_TRIANGLES, attrs, dark)
            .box(2.35f, -0.04f, 0.2f, 3.1f, 0.09f, 2.2f)

        // Vertical stabilizers
        mb.part("lstab", GL20.GL_TRIANGLES, attrs, dark)
            .box(-0.38f, 0.28f, 2.0f, 0.06f, 0.42f, 1.1f)
        mb.part("rstab", GL20.GL_TRIANGLES, attrs, dark)
            .box(0.38f, 0.28f, 2.0f, 0.06f, 0.42f, 1.1f)

        // Cockpit
        mb.part("cockpit", GL20.GL_TRIANGLES, attrs, glow)
            .box(0f, 0.32f, -1.2f, 0.48f, 0.16f, 1.0f)

        // Engine housings (no glow spheres — exhaust particles handle flame)
        mb.part("engL", GL20.GL_TRIANGLES, attrs, dark)
            .box(-0.48f, -0.05f, 2.55f, 0.32f, 0.32f, 0.95f)
        mb.part("engR", GL20.GL_TRIANGLES, attrs, dark)
            .box(0.48f, -0.05f, 2.55f, 0.32f, 0.32f, 0.95f)

        return mb.end()
    }
}

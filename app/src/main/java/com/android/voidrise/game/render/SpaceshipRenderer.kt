package com.android.voidrise.game.render

import com.android.voidrise.game.entities.Spaceship
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.Vector3

class SpaceshipRenderer {

    private lateinit var modelBatch : ModelBatch
    private lateinit var model      : Model
    private lateinit var instance   : ModelInstance
    private val environment = Environment()
    private val importScale = Vector3()

    fun init() {
        modelBatch = ModelBatch()

        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.18f, 0.22f, 0.32f, 1f))
        environment.add(DirectionalLight().set(0.75f, 0.80f, 1.00f, -0.55f, -0.90f, -0.35f))
        environment.add(DirectionalLight().set(0.12f, 0.10f, 0.18f, 0.80f, 0.20f, 0.60f))

        model = ShipModelLoader.load()
        importScale.set(ShipModelLoader.importScale())
        instance = ModelInstance(model)
        instance.transform.scale(importScale.x, importScale.y, importScale.z)
    }

    fun render(ship: Spaceship, camera: Camera) {
        instance.transform.set(ship.position, ship.quaternion)
        instance.transform.scale(importScale.x, importScale.y, importScale.z)

        modelBatch.begin(camera)
        modelBatch.render(instance, environment)
        modelBatch.end()
    }

    fun dispose() {
        if (::modelBatch.isInitialized) modelBatch.dispose()
        if (::model.isInitialized)      model.dispose()
    }
}

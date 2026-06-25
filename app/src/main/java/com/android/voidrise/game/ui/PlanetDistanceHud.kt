package com.android.voidrise.game.ui

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3

/**
 * NMS-style distance readout projected onto the planet disc.
 * 1 world unit = 1 km.
 */
object PlanetDistanceHud {

    const val UNITS_PER_KM = 1f

    private val tmp      = Vector3()
    private val toPlanet = Vector3()
    private val nameLay  = GlyphLayout()
    private val distLay  = GlyphLayout()

    data class Label(
        val screenX: Float,
        val screenY: Float,
        val alpha: Float,
        val distKm: Long,
        val planetName: String,
    )

    fun compute(
        camera: PerspectiveCamera,
        shipPos: Vector3,
        planetPos: Vector3,
        planetRadius: Float,
        planetName: String,
        nameFont: BitmapFont,
        distFont: BitmapFont,
        sw: Float,
        sh: Float,
    ): Label? {
        toPlanet.set(planetPos).sub(camera.position)
        if (toPlanet.dot(camera.direction) <= 0f) return null

        tmp.set(planetPos)
        camera.project(tmp)
        if (tmp.z < 0f || tmp.z > 1f) return null
        if (tmp.x < -80f || tmp.x > sw + 80f || tmp.y < -80f || tmp.y > sh + 80f) return null

        val centerDist  = shipPos.dst(planetPos)
        val surfaceDist = (centerDist - planetRadius).coerceAtLeast(0f)
        val distKm      = (surfaceDist * UNITS_PER_KM).toLong()

        val angularR = Math.toDegrees(
            kotlin.math.atan((planetRadius / centerDist.coerceAtLeast(1f)).toDouble()),
        ).toFloat()
        val alpha = when {
            angularR > 42f -> 0.35f
            angularR > 28f -> 0.65f
            else           -> 1f
        }

        return Label(tmp.x, tmp.y, alpha, distKm, planetName)
    }

    fun formatKm(km: Long): String = when {
        km >= 1_000_000L -> String.format("%.2f Mm", km / 1_000_000.0)
        km >= 1_000L     -> String.format("%,d km", km)
        else             -> "$km km"
    }

    private fun measure(label: Label, nameFont: BitmapFont, distFont: BitmapFont): Box {
        nameLay.setText(nameFont, label.planetName.uppercase())
        distLay.setText(distFont, formatKm(label.distKm))
        val padH = 14f
        val padV = 10f
        val w = maxOf(nameLay.width, distLay.width) + padH * 2f
        val h = nameLay.height + distLay.height + padV * 2f + 4f
        return Box(w, h, padV)
    }

    private data class Box(val w: Float, val h: Float, val padV: Float)

    fun drawBackground(
        shapes: ShapeRenderer,
        label: Label,
        nameFont: BitmapFont,
        distFont: BitmapFont,
    ) {
        val box = measure(label, nameFont, distFont)
        val bx  = label.screenX - box.w * 0.5f
        val by  = label.screenY - box.h * 0.5f

        shapes.color.set(0.02f, 0.06f, 0.14f, 0.72f * label.alpha)
        shapes.rect(bx, by, box.w, box.h)
        shapes.color.set(0.25f, 0.70f, 1.0f, 0.55f * label.alpha)
        shapes.rect(bx, by + box.h - 2f, box.w, 2f)
    }

    fun drawText(
        batch: SpriteBatch,
        label: Label,
        nameFont: BitmapFont,
        distFont: BitmapFont,
    ) {
        val box = measure(label, nameFont, distFont)
        val bx  = label.screenX - box.w * 0.5f
        val by  = label.screenY - box.h * 0.5f
        val a   = label.alpha

        nameFont.color.set(0.55f, 0.92f, 1f, 0.95f * a)
        nameFont.draw(batch, nameLay, label.screenX - nameLay.width * 0.5f, by + box.h - box.padV)

        distFont.color.set(1f, 1f, 1f, 0.92f * a)
        distFont.draw(batch, distLay, label.screenX - distLay.width * 0.5f, by + box.padV)
    }
}

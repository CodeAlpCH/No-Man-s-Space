package com.android.voidrise.game.world

import com.android.voidrise.game.entities.Matter
import com.android.voidrise.game.entities.MatterType
import com.badlogic.gdx.math.Vector2
import kotlin.random.Random

class WorldChunk(
    val chunkX: Int,
    val chunkY: Int,
) {
    companion object {
        const val SIZE = 900f
    }

    val originX: Float = chunkX * SIZE
    val originY: Float = chunkY * SIZE
    val matter: MutableList<Matter> = mutableListOf()

    private val random = Random((chunkX.toLong() * 73856093L) xor (chunkY.toLong() * 19349663L))

    init { generate() }

    private fun generate() {
        repeat(20 + random.nextInt(16)) { matter += spawn(MatterType.DUST)    }
        repeat(3  + random.nextInt(5))  { matter += spawn(MatterType.ASTEROID) }
        if (random.nextFloat() < 0.45f) matter += spawn(MatterType.STAR_REMNANT)
        if (random.nextFloat() < 0.12f) matter += spawn(MatterType.PLANET)
    }

    private fun spawn(type: MatterType): Matter {
        val x   = originX + 60f + random.nextFloat() * (SIZE - 120f)
        val y   = originY + 60f + random.nextFloat() * (SIZE - 120f)
        val y3D = (random.nextFloat() - 0.5f) * when (type) {
            MatterType.DUST         -> 8f
            MatterType.ASTEROID     -> 14f
            MatterType.STAR_REMNANT -> 20f
            MatterType.PLANET       -> 10f
        }
        val drift = Vector2(
            random.nextFloat() * 16f - 8f,
            random.nextFloat() * 16f - 8f,
        )
        return Matter(Vector2(x, y), drift, type, y3D)
    }
}

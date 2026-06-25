package com.android.voidrise.game.world

import com.android.voidrise.game.entities.Matter
import com.android.voidrise.game.entities.Player
import com.badlogic.gdx.graphics.Color
import kotlin.math.floor

class ChunkManager {
    private val loadedChunks = LinkedHashMap<Long, WorldChunk>()
    private val activeMatter = mutableListOf<Matter>()

    /** New: update using raw world XZ coordinates (for spaceship mode). */
    fun update(worldX: Float, worldZ: Float) {
        val cx = floor(worldX / WorldChunk.SIZE).toInt()
        val cy = floor(worldZ / WorldChunk.SIZE).toInt()

        val needed = hashSetOf<Long>()
        for (dx in -1..1) for (dy in -1..1) {
            val key = chunkKey(cx + dx, cy + dy)
            needed += key
            if (!loadedChunks.containsKey(key))
                loadedChunks[key] = WorldChunk(cx + dx, cy + dy)
        }

        loadedChunks.keys.retainAll(needed)

        activeMatter.clear()
        loadedChunks.values.forEach { chunk ->
            activeMatter.addAll(chunk.matter.filter { it.alive })
        }
    }

    /** Legacy: update using Player entity. */
    fun update(player: Player) {
        val cx = floor(player.position.x / WorldChunk.SIZE).toInt()
        val cy = floor(player.position.y / WorldChunk.SIZE).toInt()

        val needed = hashSetOf<Long>()
        for (dx in -1..1) for (dy in -1..1) {
            val key = chunkKey(cx + dx, cy + dy)
            needed += key
            if (!loadedChunks.containsKey(key))
                loadedChunks[key] = WorldChunk(cx + dx, cy + dy)
        }

        loadedChunks.keys.retainAll(needed)

        activeMatter.clear()
        loadedChunks.values.forEach { chunk ->
            activeMatter.addAll(chunk.matter.filter { it.alive })
        }
    }

    fun matterInRange(): List<Matter> = activeMatter

    fun nebulaColorAt(worldX: Float, worldY: Float): Color {
        val rx   = floor(worldX / (WorldChunk.SIZE * 3f)).toInt()
        val ry   = floor(worldY / (WorldChunk.SIZE * 3f)).toInt()
        val seed = (rx * 92837111) xor (ry * 689287499)
        val hue  = ((seed ushr 8) and 0xFF) / 255f
        return when {
            hue < 0.33f -> Color(0.12f + hue, 0.18f, 0.32f, 0.55f)
            hue < 0.66f -> Color(0.35f, 0.12f + hue * 0.2f, 0.28f, 0.5f)
            else        -> Color(0.28f, 0.18f, 0.08f + hue * 0.15f, 0.45f)
        }
    }

    private fun chunkKey(x: Int, y: Int): Long =
        (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)
}

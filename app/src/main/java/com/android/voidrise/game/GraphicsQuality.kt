package com.android.voidrise.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector3
import com.android.voidrise.game.world.WorldPlanet

/**
 * Auto-detects device tier so Fold-class phones keep full visuals,
 * budget phones (A16 etc.) use cheaper planet shaders + mesh LOD.
 */
object GraphicsQuality {

    enum class Tier { LOW, HIGH }

    lateinit var tier: Tier
        private set

    val planetMeshSegments: Int
        get() = if (tier == Tier.HIGH) 72 else 40

    /** Only applies once inside mesh LOD range (very close). */
    fun planetShaderDetail(shipPos: Vector3): Float {
        val surfaceDist = WorldPlanet.surfaceDistance(shipPos)
        if (surfaceDist > 800f) return 0f
        if (tier == Tier.HIGH) return 1f
        return 0f
    }

    fun detect() {
        val g = Gdx.graphics
        val pixels = g.width.toLong() * g.height
        val heapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val density = g.density

        // Budget phones: small heap + moderate resolution (A16 class)
        val lowEnd = heapMb < 384 && pixels < 2_800_000L
        val flagship = pixels >= 3_200_000L || (heapMb >= 512 && density >= 3f)

        tier = when {
            flagship -> Tier.HIGH
            lowEnd   -> Tier.LOW
            else     -> Tier.HIGH
        }

        Gdx.app.log(
            "GraphicsQuality",
            "tier=$tier pixels=$pixels heapMb=$heapMb density=$density",
        )
    }
}

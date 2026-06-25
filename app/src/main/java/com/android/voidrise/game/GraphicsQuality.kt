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
        get() = if (tier == Tier.HIGH) 72 else 28

    val cloudShellScale: Float
        get() = if (tier == Tier.HIGH) 1f else 0f

    val atmosphereShellScale: Float
        get() = if (tier == Tier.HIGH) 1f else 0.35f

    val atmosphereFogAlphaScale: Float
        get() = if (tier == Tier.HIGH) 1f else 0.45f

    val atmosphereCloudStrength: Float
        get() = if (tier == Tier.HIGH) 1f else 0f

    /** Ramps surface noise in as the planet fills the view, avoiding a flat blue mesh band. */
    fun planetShaderDetail(shipPos: Vector3): Float {
        val surfaceDist = WorldPlanet.surfaceDistance(shipPos)
        val detail = 1f - ((surfaceDist - DETAIL_FULL_KM) / (WorldPlanet.MESH_LOD_SURFACE_KM - DETAIL_FULL_KM))
            .coerceIn(0f, 1f)
        return if (tier == Tier.HIGH) detail else 0f
    }

    fun detect() {
        val g = Gdx.graphics
        val pixels = g.width.toLong() * g.height
        val heapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val density = g.density

        // Budget phones: A16-class devices need cheaper atmosphere/water paths.
        val lowEnd = (heapMb <= 512 && pixels < 3_000_000L) || (pixels < 2_800_000L && density <= 3f)
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

    private const val DETAIL_FULL_KM = 350f
}

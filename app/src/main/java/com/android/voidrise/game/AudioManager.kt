package com.android.voidrise.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

/**
 * ASMR Audio-System.
 *
 * Erwartet folgende Assets unter assets/sounds/:
 *   ambient.ogg      – tiefer, kontinuierlicher Weltraum-Drone
 *   absorb_dust.ogg  – sanftes Saugen (kleine Partikel)
 *   absorb_big.ogg   – tiefes Rumble (Asteroids/Planeten)
 *   boost.ogg        – hochfrequenter Jet-Whoosh
 *   grow.ogg         – satter Bass-Thud beim Massezuwachs
 *
 * Alle Sounds sind optional – falls Datei fehlt, wird sie still ignoriert.
 */
class AudioManager {
    private var ambient   : Music? = null
    private var absorbDust: Sound? = null
    private var absorbBig : Sound? = null
    private var boost     : Sound? = null
    private var grow      : Sound? = null

    fun init() {
        ambient    = tryMusic("sounds/ambient.ogg")
        absorbDust = trySound("sounds/absorb_dust.ogg")
        absorbBig  = trySound("sounds/absorb_big.ogg")
        boost      = trySound("sounds/boost.ogg")
        grow       = trySound("sounds/grow.ogg")

        ambient?.apply {
            isLooping = true
            volume    = 0.35f
            play()
        }
    }

    fun playAbsorb(massGained: Float) {
        if (massGained > 20f) {
            absorbBig?.play((0.65f + massGained * 0.004f).coerceAtMost(1f))
            if (massGained > 50f) grow?.play(0.7f)
        } else {
            absorbDust?.play((0.45f + massGained * 0.02f).coerceAtMost(1f))
        }
    }

    fun playBoost() { boost?.play(0.8f) }

    fun setAmbientVolume(v: Float) { ambient?.volume = v.coerceIn(0f, 1f) }

    fun dispose() {
        ambient?.dispose()
        absorbDust?.dispose()
        absorbBig?.dispose()
        boost?.dispose()
        grow?.dispose()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun tryMusic(path: String): Music? = try {
        val file = Gdx.files.internal(path)
        if (file.exists()) Gdx.audio.newMusic(file) else null
    } catch (e: Exception) { null }

    private fun trySound(path: String): Sound? = try {
        val file = Gdx.files.internal(path)
        if (file.exists()) Gdx.audio.newSound(file) else null
    } catch (e: Exception) { null }
}

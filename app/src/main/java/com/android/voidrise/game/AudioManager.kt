package com.android.voidrise.game

import com.android.voidrise.game.audio.ProceduralEngineSound
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.MathUtils

/**
 * ASMR Audio-System.
 *
 * Erwartet folgende Assets unter assets/sounds/:
 *   ambient.ogg         – tiefer, kontinuierlicher Weltraum-Drone
 *   absorb_dust.ogg     – sanftes Saugen (kleine Partikel)
 *   absorb_big.ogg      – tiefes Rumble (Asteroids/Planeten)
 *   boost.ogg           – hochfrequenter Jet-Whoosh
 *   grow.ogg            – satter Bass-Thud beim Massezuwachs
 *   warp_3seconds.wav   – Warp-Aufladesound (3 s Countdown, fadet am Ende aus)
 *
 * Triebwerk: prozedural (ProceduralEngineSound) — kein WAV nötig.
 * Alle Datei-Sounds sind optional — fehlende Dateien werden ignoriert.
 */
class AudioManager {

    private var ambient   : Music? = null
    private var absorbDust: Sound? = null
    private var absorbBig : Sound? = null
    private var boost     : Sound? = null
    private var grow      : Sound? = null
    private var warpCharge: Sound? = null
    private var warpChargeId = -1L

    private var engine: ProceduralEngineSound? = null

    fun init() {
        ambient    = tryMusic("sounds/ambient.ogg")
        absorbDust = trySound("sounds/absorb_dust.ogg")
        absorbBig  = trySound("sounds/absorb_big.ogg")
        boost      = trySound("sounds/boost.ogg")
        grow       = trySound("sounds/grow.ogg")
        warpCharge = trySound("sounds/warp_3seconds.wav")

        try {
            engine = ProceduralEngineSound()
        } catch (_: Exception) {
            engine = null
        }

        ambient?.apply {
            isLooping = true
            volume    = 0.35f
            play()
        }
    }

    fun updateEngine(throttle: Float, warpEngaged: Boolean, dt: Float) {
        engine?.update(
            throttle = throttle,
            active = !warpEngaged,
            dt = dt,
        )
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

    fun playWarpCharge() {
        val sound = warpCharge ?: return
        if (warpChargeId != -1L) sound.stop(warpChargeId)
        warpChargeId = sound.play(WARP_CHARGE_PEAK)
    }

    fun updateWarpCharge(countdownRemainingSec: Float, charging: Boolean) {
        val sound = warpCharge ?: return
        if (warpChargeId == -1L) return

        if (!charging) {
            sound.stop(warpChargeId)
            warpChargeId = -1L
            return
        }

        val vol = if (countdownRemainingSec <= WARP_CHARGE_FADE_SEC) {
            (countdownRemainingSec / WARP_CHARGE_FADE_SEC).coerceIn(0f, 1f)
        } else {
            1f
        }
        sound.setVolume(warpChargeId, vol * WARP_CHARGE_PEAK)
        if (countdownRemainingSec <= 0.02f) {
            sound.stop(warpChargeId)
            warpChargeId = -1L
        }
    }

    fun stopWarpCharge() {
        if (warpChargeId == -1L) return
        warpCharge?.stop(warpChargeId)
        warpChargeId = -1L
    }

    fun setAmbientVolume(v: Float) { ambient?.volume = v.coerceIn(0f, 1f) }

    fun dispose() {
        ambient?.dispose()
        absorbDust?.dispose()
        absorbBig?.dispose()
        boost?.dispose()
        grow?.dispose()
        warpCharge?.dispose()
        engine?.dispose()
    }

    companion object {
        private const val WARP_CHARGE_PEAK     = 0.88f
        private const val WARP_CHARGE_FADE_SEC = 0.85f
    }

    private fun tryMusic(path: String): Music? = try {
        val file = Gdx.files.internal(path)
        if (file.exists()) Gdx.audio.newMusic(file) else null
    } catch (_: Exception) {
        null
    }

    private fun trySound(path: String): Sound? = try {
        val file = Gdx.files.internal(path)
        if (file.exists()) Gdx.audio.newSound(file) else null
    } catch (_: Exception) {
        null
    }
}

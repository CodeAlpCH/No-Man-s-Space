package com.android.voidrise.game.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.MathUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Sci-fi engine hum — synthesized at runtime, no WAV asset needed.
 * Three loop layers (low / mid / high) crossfade with throttle.
 */
class ProceduralEngineSound {

    private val low: Sound
    private val mid: Sound
    private val high: Sound

    private var lowId = -1L
    private var midId = -1L
    private var highId = -1L

    private var smoothT = 0f

    init {
        low  = buildLoop("eng_low",  42f,  0.10f, 0.95f)
        mid  = buildLoop("eng_mid",  78f,  0.16f, 0.88f)
        high = buildLoop("eng_high", 118f, 0.24f, 0.82f)
    }

    fun update(throttle: Float, active: Boolean, dt: Float) {
        val target = if (active) throttle.coerceIn(0f, 1f) else 0f
        smoothT = MathUtils.lerp(smoothT, target, (dt * 7f).coerceAtMost(1f))

        if (smoothT < 0.02f) {
            stopAll()
            return
        }

        if (lowId == -1L) {
            lowId  = low.loop(0f)
            midId  = mid.loop(0f)
            highId = high.loop(0f)
        }

        val t = smoothT
        val wLow  = (1f - t).toDouble().pow(1.35).toFloat()
        val wMid  = MathUtils.clamp(1f - kotlin.math.abs(t - 0.48f) * 2.1f, 0f, 1f).let { it * it }
        val wHigh = t.toDouble().pow(1.55).toFloat()
        val norm  = (wLow + wMid + wHigh).coerceAtLeast(0.001f)

        val master = 0.14f + t * t * 0.58f
        low.setVolume(lowId,  wLow  / norm * master * 0.95f)
        mid.setVolume(midId,  wMid  / norm * master * 0.90f)
        high.setVolume(highId, wHigh / norm * master * 0.88f)
    }

    fun dispose() {
        stopAll()
        low.dispose()
        mid.dispose()
        high.dispose()
    }

    private fun stopAll() {
        if (lowId != -1L)  low.stop(lowId)
        if (midId != -1L)  mid.stop(midId)
        if (highId != -1L) high.stop(highId)
        lowId = -1L
        midId = -1L
        highId = -1L
    }

    private fun buildLoop(cacheKey: String, baseHz: Float, noise: Float, gain: Float): Sound {
        val rate = 44100
        val seconds = 2.5f
        val count = (rate * seconds).toInt()
        val pcm = ShortArray(count)

        var p1 = 0.0
        var p2 = 0.0
        var pSub = 0.0
        var lp = 0f
        var breath = 0f

        for (i in 0 until count) {
            val t = i.toDouble() / rate
            p1   += baseHz / rate
            p2   += (baseHz * 2.01) / rate
            pSub += (baseHz * 0.49) / rate

            breath = breath * 0.985f + (Math.random().toFloat() - 0.5f) * 0.015f
            lp = lp * 0.90f + (Math.random().toFloat() - 0.5f) * 0.10f

            val body = sin(p1 * PI * 2) * 0.55 +
                       sin(p2 * PI * 2) * 0.22 +
                       sin(pSub * PI * 2) * 0.38
            val am = 0.86 + 0.14 * sin(t * 4.9)

            var sample = (body + lp * noise + breath * 0.06) * am * gain
            sample = sample.coerceIn(-1.0, 1.0)
            pcm[i] = (sample * 26000.0).toInt().toShort()
        }

        val file = Gdx.files.local("voidrise_audio/$cacheKey.wav")
        file.parent().mkdirs()
        file.writeBytes(encodeWav(pcm, rate), false)
        return Gdx.audio.newSound(file)
    }

    /** Minimal PCM16 mono WAV. */
    private fun encodeWav(samples: ShortArray, sampleRate: Int): ByteArray {
        val dataSize = samples.size * 2
        val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray())
        buf.putInt(36 + dataSize)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16)
        buf.putShort(1) // PCM
        buf.putShort(1) // mono
        buf.putInt(sampleRate)
        buf.putInt(sampleRate * 2)
        buf.putShort(2)
        buf.putShort(16)
        buf.put("data".toByteArray())
        buf.putInt(dataSize)
        for (s in samples) buf.putShort(s)
        return buf.array()
    }
}

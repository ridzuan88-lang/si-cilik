package com.chewanninja.misisicilik

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** All audio is synthesized in code: no audio files, fully offline, no licensing issues. */
object Audio {
    private const val SR = 22050
    @Volatile var sfxOn = true
    @Volatile var musicOn = true
    private var music: AudioTrack? = null
    private val handler = Handler(Looper.getMainLooper())
    private val cache = HashMap<String, ShortArray>()

    private fun sweep(f0: Float, f1: Float, ms: Int, vol: Float = 0.5f): ShortArray {
        val n = SR * ms / 1000
        val out = ShortArray(n)
        var ph = 0.0
        for (i in 0 until n) {
            val p = i / n.toFloat()
            ph += 2.0 * PI * (f0 + (f1 - f0) * p) / SR
            val env = minOf(1f, i / 150f) * (1f - p).pow(1.4f)
            out[i] = (sin(ph) * env * vol * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun cat(vararg a: ShortArray): ShortArray {
        val o = ShortArray(a.sumOf { it.size }); var k = 0
        for (x in a) { x.copyInto(o, k); k += x.size }
        return o
    }

    private fun noise(ms: Int): ShortArray {
        val n = SR * ms / 1000; val r = java.util.Random(3)
        return ShortArray(n) { i -> ((r.nextFloat() * 2 - 1) * (1f - i / n.toFloat()) * 0.5f * 32767).toInt().toShort() }
    }

    private fun sound(name: String): ShortArray = cache.getOrPut(name) {
        when (name) {
            "jump" -> sweep(350f, 820f, 150)
            "coin" -> cat(sweep(988f, 988f, 70), sweep(1319f, 1319f, 170))
            "star" -> cat(sweep(784f, 784f, 70), sweep(988f, 988f, 70), sweep(1319f, 1319f, 70), sweep(1568f, 1568f, 200))
            "win" -> cat(sweep(523f, 523f, 130), sweep(659f, 659f, 130), sweep(784f, 784f, 130), sweep(1047f, 1047f, 380))
            "lose" -> cat(sweep(440f, 380f, 200), sweep(330f, 200f, 420))
            "hit" -> sweep(220f, 90f, 220)
            "pop" -> noise(90)
            else -> sweep(720f, 520f, 60, 0.35f)
        }
    }

    private fun track(data: ShortArray): AudioTrack =
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

    fun play(name: String) {
        if (!sfxOn) return
        try {
            val d = sound(name)
            val t = track(d)
            t.write(d, 0, d.size)
            t.play()
            handler.postDelayed({ try { t.release() } catch (e: Throwable) { } }, d.size * 1000L / SR + 250)
        } catch (e: Throwable) { }
    }

    private fun buildMusic(): ShortArray {
        val scale = floatArrayOf(523.25f, 587.33f, 659.25f, 783.99f, 880f)
        val melody = intArrayOf(0, 2, 4, 3, 2, 1, 0, 2, 4, 3, 2, 1, 0, 1, 2, 0, 2, 4, 3, 4, 2, 1, 0, 1, 3, 2, 1, 0, 2, 3, 4, 2)
        val step = SR * 270 / 1000
        val out = ShortArray(step * melody.size)
        for ((i, m) in melody.withIndex()) {
            val f = scale[m].toDouble()
            for (k in 0 until step) {
                val p = k / step.toFloat()
                val env = minOf(1f, k / 120f) * (1f - p).pow(0.8f)
                val bass = if (i % 4 == 0) sin(2.0 * PI * (f / 4.0) * k / SR) * 0.5 else 0.0
                val s = (sin(2.0 * PI * f * k / SR) + 0.3 * sin(2.0 * PI * 2.0 * f * k / SR)) * 0.6 + bass
                out[i * step + k] = (s * env * 0.16 * 32767.0).toInt().toShort()
            }
        }
        return out
    }

    fun startMusic() {
        if (!musicOn) return
        try {
            if (music == null) {
                val d = buildMusic()
                val t = track(d)
                t.write(d, 0, d.size)
                t.setLoopPoints(0, d.size, -1)
                music = t
            }
            music?.play()
        } catch (e: Throwable) { }
    }

    fun pauseMusic() { try { music?.pause() } catch (e: Throwable) { } }

    fun applyMusic() { if (musicOn) startMusic() else pauseMusic() }
}

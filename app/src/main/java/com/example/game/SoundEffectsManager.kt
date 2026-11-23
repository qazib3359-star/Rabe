package com.example.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ultra-low-RAM synthesized arcade sound effects & haptic manager.
 * Uses pre-computed 22.05 kHz 16-bit mono PCM buffers (~120 KB total RAM)
 * so zero external audio files or heavy MediaPlayer pools are needed.
 */
class SoundEffectsManager(context: Context) {
    private val appContext = context.applicationContext
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var hapticsEnabled: Boolean = true

    @Volatile
    private var released = false

    private val shootBuffer: ShortArray = synthesizeSweep(
        durationMs = 95,
        startFreq = 320.0,
        endFreq = 780.0,
        volume = 0.42
    )
    private val bounceBuffer: ShortArray = synthesizeSweep(
        durationMs = 55,
        startFreq = 520.0,
        endFreq = 260.0,
        volume = 0.30
    )
    private val swapBuffer: ShortArray = synthesizeDualTone(
        durationMs = 85,
        freq1 = 440.0,
        freq2 = 660.0,
        volume = 0.32
    )
    private val buttonBuffer: ShortArray = synthesizeSweep(
        durationMs = 42,
        startFreq = 640.0,
        endFreq = 860.0,
        volume = 0.28
    )
    private val popBuffers: List<ShortArray> = List(5) { idx ->
        val baseFreq = 480.0 + idx * 115.0
        synthesizePop(durationMs = 110, baseFreq = baseFreq, volume = 0.50)
    }
    private val dropBuffer: ShortArray = synthesizeArpeggio(
        notes = doubleArrayOf(587.33, 783.99, 1046.50),
        noteMs = 55,
        volume = 0.38
    )
    private val winBuffer: ShortArray = synthesizeArpeggio(
        notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51),
        noteMs = 95,
        volume = 0.48
    )
    private val loseBuffer: ShortArray = synthesizeArpeggio(
        notes = doubleArrayOf(440.0, 392.0, 349.23, 261.63),
        noteMs = 115,
        volume = 0.40
    )

    fun playShoot() = playPcm(shootBuffer)
    fun playBounce() = playPcm(bounceBuffer)
    fun playSwap() = playPcm(swapBuffer)
    fun playButton() = playPcm(buttonBuffer)
    fun playDrop() = playPcm(dropBuffer)
    fun playWin() {
        playPcm(winBuffer)
        vibratePulse(45L)
    }
    fun playLose() {
        playPcm(loseBuffer)
        vibratePulse(65L)
    }
    fun playPop(combo: Int = 1) {
        val idx = (combo - 1).coerceIn(0, popBuffers.lastIndex)
        playPcm(popBuffers[idx])
        vibratePulse(if (combo >= 2) 32L else 18L)
    }

    private fun vibratePulse(durationMs: Long) {
        if (!hapticsEnabled || released) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Vibration skipped: ${e.message}")
        }
    }

    private fun playPcm(samples: ShortArray) {
        if (!soundEnabled || released) return
        executor.execute {
            if (released || !soundEnabled) return@execute
            var track: AudioTrack? = null
            try {
                val byteCount = samples.size * 2
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(byteCount)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                val sleepMs = (samples.size * 1000L / SAMPLE_RATE) + 25L
                Thread.sleep(sleepMs)
            } catch (e: Exception) {
                Log.d(TAG, "AudioTrack playback skipped: ${e.message}")
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun release() {
        released = true
        executor.shutdownNow()
    }

    companion object {
        private const val TAG = "SoundFX"
        private const val SAMPLE_RATE = 22050

        private fun synthesizeSweep(
            durationMs: Int,
            startFreq: Double,
            endFreq: Double,
            volume: Double
        ): ShortArray {
            val count = (SAMPLE_RATE * durationMs) / 1000
            val out = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = startFreq + (endFreq - startFreq) * t
                val env = sin(PI * t) * exp(-2.0 * t)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val sample = sin(phase) * env * volume
                out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return out
        }

        private fun synthesizePop(durationMs: Int, baseFreq: Double, volume: Double): ShortArray {
            val count = (SAMPLE_RATE * durationMs) / 1000
            val out = ShortArray(count)
            val rng = Random(42)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = baseFreq * (1.0 + 0.65 * t)
                val env = exp(-5.2 * t)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val burstNoise = if (t < 0.12) (rng.nextDouble() * 2.0 - 1.0) * 0.25 else 0.0
                val sample = (sin(phase) * 0.85 + sin(phase * 2.0) * 0.15 + burstNoise) * env * volume
                out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return out
        }

        private fun synthesizeDualTone(
            durationMs: Int,
            freq1: Double,
            freq2: Double,
            volume: Double
        ): ShortArray {
            val count = (SAMPLE_RATE * durationMs) / 1000
            val out = ShortArray(count)
            val mid = count / 2
            var phase = 0.0
            for (i in 0 until count) {
                val freq = if (i < mid) freq1 else freq2
                val localT = (if (i < mid) i else i - mid).toDouble() / mid
                val env = sin(PI * localT)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val sample = sin(phase) * env * volume
                out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return out
        }

        private fun synthesizeArpeggio(
            notes: DoubleArray,
            noteMs: Int,
            volume: Double
        ): ShortArray {
            val samplesPerNote = (SAMPLE_RATE * noteMs) / 1000
            val out = ShortArray(samplesPerNote * notes.size)
            for (nIdx in notes.indices) {
                val freq = notes[nIdx]
                var phase = 0.0
                for (i in 0 until samplesPerNote) {
                    val t = i.toDouble() / samplesPerNote
                    val env = sin(PI * t) * exp(-1.6 * t)
                    phase += 2.0 * PI * freq / SAMPLE_RATE
                    val sample = (sin(phase) * 0.78 + sin(phase * 2.0) * 0.22) * env * volume
                    out[nIdx * samplesPerNote + i] =
                        (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            return out
        }
    }
}

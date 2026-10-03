package io.adambailey.framebeat.engine

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin

/**
 * One strike of a [VoiceSpec], rendered a block at a time. The offline render
 * and the live output both go through [Mixer], which renders voices this way,
 * so a hit sounds the same in a test as it does on the device.
 *
 * Stateful (oscillator phase, filter memory): frames come out in order, once.
 */
internal class Voice(spec: VoiceSpec, sampleRate: Int, noise: DoubleArray, private val startFrame: Long) {
    // An array, read by index: the audio thread does not allocate an iterator per frame.
    private val parts: Array<Part> = (
        spec.oscillators.map { OscillatorPart(it, sampleRate) } +
            spec.noises.map { NoisePart(it, sampleRate, noise) }
        ).toTypedArray()

    /** Frames from the start until the last part stops. */
    private val length = parts.maxOf { it.length }

    /** Frames rendered so far. */
    private var position = 0

    val finished: Boolean get() = position >= length

    /**
     * Adds this voice into [mix], where `mix[0]` is frame [blockStart] and [frames] frames are due.
     * A voice whose start frame has already passed starts at the top of the block.
     */
    fun mixInto(mix: DoubleArray, blockStart: Long, frames: Int) {
        var i = max(0L, startFrame - blockStart)
        while (i < frames && position < length) {
            var sum = 0.0
            for (p in parts.indices) {
                val part = parts[p]
                if (position < part.length) sum += part.next()
            }
            mix[i.toInt()] += sum
            position++
            i++
        }
    }
}

/** An oscillator or noise band that produces [length] frames, one per [next] call. */
private sealed class Part(stop: Double, sampleRate: Int) {
    // Web Audio's `stop(t + stop)`: frames before that time sound.
    val length = ceil(stop * sampleRate).toInt()

    abstract fun next(): Double
}

/**
 * A phase-accumulating oscillator, so a frequency glide stays in tune. Starts at
 * phase 0, as a Web Audio oscillator does. The triangle sums its odd harmonics
 * below Nyquist, like Web Audio's band-limited triangle, rather than a naive
 * ramp that would alias at the click's 1450 Hz.
 */
private class OscillatorPart(private val spec: OscillatorSpec, sampleRate: Int) : Part(spec.stop, sampleRate) {
    private val dt = 1.0 / sampleRate
    private val nyquist = sampleRate / 2.0
    private var phase = 0.0
    private var index = 0

    override fun next(): Double {
        val t = index * dt
        val freq = spec.freq.value(t)
        val wave = when (spec.waveform) {
            Waveform.Sine -> sin(phase)
            Waveform.Triangle -> triangle(phase, freq)
        }
        phase += 2 * PI * freq * dt
        if (phase >= 2 * PI) phase -= 2 * PI
        index++
        return wave * spec.gain.value(t)
    }

    // 8 / pi^2 * sum over odd n of (-1)^((n-1)/2) sin(n * phase) / n^2.
    private fun triangle(phase: Double, freq: Double): Double {
        var sum = 0.0
        var n = 1
        var sign = 1.0
        while (n * freq < nyquist) {
            sum += sign * sin(n * phase) / (n * n)
            sign = -sign
            n += 2
        }
        return sum * TRIANGLE_SCALE
    }

    private companion object {
        const val TRIANGLE_SCALE = 8 / (PI * PI)
    }
}

/** Looped noise from the shared table, read from its start, through a fresh filter. */
private class NoisePart(private val spec: NoiseSpec, private val sampleRate: Int, private val noise: DoubleArray) :
    Part(spec.stop, sampleRate) {
    private val filter = Biquad(spec.filter, spec.frequency, spec.q, sampleRate)
    private var index = 0

    override fun next(): Double {
        val filtered = filter.process(noise[index % noise.size])
        val gain = spec.gain.value(index.toDouble() / sampleRate)
        index++
        return filtered * gain
    }
}

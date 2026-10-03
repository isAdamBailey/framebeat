package io.adambailey.framebeat.engine

import kotlin.math.pow

/**
 * The subset of Web Audio `AudioParam` automation `src/lib/drumAudio.ts` uses:
 * the first breakpoint is a `setValueAtTime`, every later one an
 * `exponentialRampToValueAtTime`. Times are seconds after the strike.
 * Port of `envelopeValue` in src/lib/voiceSpec.ts.
 */
class Envelope(vararg points: Pair<Double, Double>) {
    private val times = DoubleArray(points.size) { points[it].first }
    private val values = DoubleArray(points.size) { points[it].second }

    init {
        require(points.isNotEmpty()) { "an envelope needs at least one breakpoint" }
        // Web Audio rejects an exponential ramp to or from zero.
        require(values.all { it > 0 }) { "exponential ramps need positive values" }
    }

    /** The breakpoints as (time, value) pairs. */
    val points: List<Pair<Double, Double>> get() = times.indices.map { times[it] to values[it] }

    /** The time of the last breakpoint. */
    val end: Double get() = times.last()

    /**
     * The value Web Audio gives [t] seconds after the strike: held before the
     * first point and after the last, and between points
     * v0 * (v1 / v0) ^ ((t - t0) / (t1 - t0)).
     */
    fun value(t: Double): Double {
        var t0 = times[0]
        var v0 = values[0]
        if (t <= t0) return v0
        for (i in 1 until times.size) {
            val t1 = times[i]
            val v1 = values[i]
            if (t <= t1) return if (t1 == t0) v1 else v0 * (v1 / v0).pow((t - t0) / (t1 - t0))
            t0 = t1
            v0 = v1
        }
        return v0
    }
}

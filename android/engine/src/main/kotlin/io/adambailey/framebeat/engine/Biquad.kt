package io.adambailey.framebeat.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

enum class FilterType(val id: String) {
    Lowpass("lowpass"),
    Bandpass("bandpass"),
    Highpass("highpass"),
}

/**
 * One second-order IIR filter with Web Audio's `BiquadFilterNode` coefficients.
 * Q is in decibels for lowpass and highpass, and linear for bandpass, as Web
 * Audio defines it (`Biquad.swift` records the same finding, checked against
 * Chrome). A fresh filter starts at rest, like each `createBiquadFilter()` call.
 */
class Biquad(type: FilterType, frequency: Double, q: Double, sampleRate: Int) {
    private val b0: Double
    private val b1: Double
    private val b2: Double
    private val a1: Double
    private val a2: Double
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    init {
        val w0 = 2 * PI * frequency / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val effectiveQ = if (type == FilterType.Bandpass) q else 10.0.pow(q / 20)
        val alpha = sinW0 / (2 * effectiveQ)
        val a0 = 1 + alpha
        when (type) {
            FilterType.Lowpass -> {
                b0 = (1 - cosW0) / 2 / a0
                b1 = (1 - cosW0) / a0
                b2 = (1 - cosW0) / 2 / a0
            }
            FilterType.Highpass -> {
                b0 = (1 + cosW0) / 2 / a0
                b1 = -(1 + cosW0) / a0
                b2 = (1 + cosW0) / 2 / a0
            }
            // Constant 0 dB peak gain, as Web Audio's bandpass.
            FilterType.Bandpass -> {
                b0 = alpha / a0
                b1 = 0.0
                b2 = -alpha / a0
            }
        }
        a1 = -2 * cosW0 / a0
        a2 = (1 - alpha) / a0
    }

    fun process(x0: Double): Double {
        val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x0
        y2 = y1
        y1 = y0
        return y0
    }
}

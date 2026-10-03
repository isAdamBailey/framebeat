package io.adambailey.framebeat.engine

import kotlin.random.Random

/**
 * One second of white noise in [-1, 1), like drumAudio.ts's shared
 * `noiseBuffer`: every noise band reads it from the start and loops it.
 * The values are an independent random draw, so a render is not
 * sample-identical to the web.
 */
object NoiseTable {
    fun make(sampleRate: Int, random: Random = Random.Default): DoubleArray =
        DoubleArray(sampleRate) { random.nextDouble() * 2 - 1 }
}

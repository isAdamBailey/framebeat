package io.adambailey.framebeat.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The shared voice and mixer: what `EnvelopeAndSynthTests.swift` covers, plus block-size independence. */
class VoiceTest {
    private val rate = 48_000
    private val noise = NoiseTable.make(rate, Random(7))
    private val allVoices = listOf(Voices.bass, Voices.edge, Voices.click, Voices.ding)

    private fun render(seconds: Double, vararg strikes: Pair<Long, VoiceSpec>) =
        Mixer.renderOffline(rate, (seconds * rate).toInt(), strikes.toList(), noise)

    private fun FloatArray.peak(from: Int = 0, to: Int = size) = (from until to).maxOfOrNull { abs(this[it]) } ?: 0f

    private fun FloatArray.rms(from: Int, to: Int) = sqrt((from until to).sumOf { this[it].toDouble() * this[it] } / (to - from))

    @Test
    fun exponentialRampMatchesTheWebAudioFormula() {
        val env = Envelope(0.0 to 0.0001, 0.008 to 0.9)
        assertEquals(0.0001 * Math.pow(0.9 / 0.0001, 0.5), env.value(0.004), 1e-12)
        assertEquals(5.0, Envelope(0.01 to 5.0, 0.02 to 10.0).value(0.0), 0.0)
        assertEquals(2.0, Envelope(0.0 to 1.0, 0.1 to 2.0).value(1.0), 0.0)
    }

    @Test
    fun rampsToZeroAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { Envelope(0.0 to 1.0, 0.1 to 0.0) }
    }

    @Test
    fun everyVoiceSoundsAndStaysBoundedAndFinite() {
        for (spec in allVoices) {
            val out = render(2.0, 0L to spec)
            assertTrue(out.all { it.isFinite() })
            assertTrue(out.peak() <= 1f)
            assertTrue("voice should be audible", out.peak() > 0.05f)
        }
        val all = render(2.0, *allVoices.map { 0L to it }.toTypedArray())
        assertTrue(all.all { it.isFinite() && abs(it) <= 1f })
    }

    @Test
    fun nothingSoundsBeforeTheStrike() {
        for (spec in allVoices) {
            val out = render(1.0, 1000L to spec)
            assertEquals(0f, out.peak(0, 1000))
            assertTrue(out.peak(1000, 2000) > 0f)
        }
    }

    @Test
    fun eachVoiceStopsAtItsStopTime() {
        for (spec in allVoices) {
            val stopFrame = 100 + Math.ceil(spec.stop * rate).toInt()
            val out = render(2.5, 100L to spec)
            assertEquals(0f, out.peak(stopFrame, out.size))
            assertTrue(spec.oscillators.all { it.stop >= it.freq.end && it.stop >= it.gain.end })
            assertTrue(spec.noises.all { it.stop >= it.gain.end })
        }
    }

    @Test
    fun theBellOutlastsADrumHit() {
        val at = (0.9 * rate).toInt()
        val bell = render(2.0, 0L to Voices.ding)
        assertTrue(bell.rms(at, at + 480) > 1e-3)
        for (sound in Sound.entries) {
            assertEquals(0f, render(2.0, 0L to Voices.forSound(sound)).peak(at, at + 480))
        }
    }

    @Test
    fun blockSizeDoesNotChangeTheOutput() {
        val strikes = listOf(0L to Voices.bass, 700L to Voices.click, 5_000L to Voices.edge, 9_999L to Voices.ding)
        val frames = rate
        val whole = Mixer.renderOffline(rate, frames, strikes, noise)
        for (block in listOf(1, 64, 333, 1024)) {
            val mixer = Mixer(rate, noise)
            for ((at, spec) in strikes) mixer.trigger(spec, at)
            val out = FloatArray(frames)
            var offset = 0
            while (offset < frames) {
                val n = minOf(block, frames - offset)
                mixer.render(out, offset, n)
                offset += n
            }
            assertArrayEquals("block $block", whole, out, 0f)
            assertEquals(frames.toLong(), mixer.frame)
        }
    }

    @Test
    fun aLateStrikePlaysAtTheStartOfTheNextBlock() {
        val mixer = Mixer(rate, noise)
        mixer.render(FloatArray(512))
        mixer.trigger(Sound.Bass, atFrame = 100)
        val late = FloatArray(4096).also { mixer.render(it) }
        val onTime = Mixer.renderOffline(rate, 4096, listOf(0L to Voices.bass), noise)
        assertArrayEquals(onTime, late, 0f)
    }

    @Test
    fun finishedVoicesAreDropped() {
        val mixer = Mixer(rate, noise)
        mixer.trigger(Sound.Click, 0)
        mixer.trigger(Sound.Edge, 10_000)
        mixer.render(FloatArray(256))
        assertEquals(2, mixer.activeVoices)
        mixer.render(FloatArray(rate))
        assertEquals(0, mixer.activeVoices)
    }

    @Test
    fun clearSilencesSoundingAndWaitingVoicesAndKeepsTheClock() {
        val mixer = Mixer(rate, noise)
        mixer.triggerDing(0)
        mixer.render(FloatArray(256))
        mixer.trigger(Sound.Bass, 1000)
        mixer.clear()
        assertEquals(0, mixer.activeVoices)
        assertEquals(256L, mixer.frame)
        val after = FloatArray(4096).also { mixer.render(it) }
        assertEquals(0f, after.peak())
        assertEquals(256L + 4096, mixer.frame)
    }

    @Test
    fun triangleIsBandLimitedAndMatchesTheSeriesAtLowPitch() {
        // A constant 100 Hz triangle at full gain has harmonics well past 20 kHz
        // in the series, so it should be within ~1% of the ideal shape.
        val spec = VoiceSpec(
            listOf(OscillatorSpec(Waveform.Triangle, Envelope(0.0 to 100.0), Envelope(0.0 to 1.0), stop = 0.1)),
            emptyList(),
        )
        val mixer = Mixer(rate, noise)
        mixer.trigger(spec, 0)
        val out = FloatArray(rate / 10).also { mixer.render(it) }
        for (i in out.indices step 37) {
            val phase = 2 * PI * 100 * i / rate
            val ideal = 2 / PI * Math.asin(sin(phase))
            assertEquals("frame $i", kotlin.math.tanh(ideal * Mixer.MASTER_GAIN), out[i].toDouble(), 0.01)
        }
    }

    @Test
    fun lowpassPassesDcAndHighpassBlocksIt() {
        val lowpass = Biquad(FilterType.Lowpass, 350.0, 1.0, rate)
        val highpass = Biquad(FilterType.Highpass, 2200.0, 1.0, rate)
        var lo = 0.0
        var hi = 0.0
        repeat(rate) {
            lo = lowpass.process(1.0)
            hi = highpass.process(1.0)
        }
        assertEquals(1.0, lo, 1e-6)
        assertEquals(0.0, hi, 1e-6)
    }

    @Test
    fun bandpassPeaksAtZeroDecibels() {
        val filter = Biquad(FilterType.Bandpass, 700.0, 0.9, rate)
        var peak = 0.0
        for (i in 0 until rate) {
            val y = filter.process(sin(2 * PI * 700 * i / rate))
            if (i > rate / 2) peak = maxOf(peak, abs(y))
        }
        assertEquals(1.0, peak, 1e-3)
    }

    @Test
    fun noiseTableIsOneSecondInRange() {
        val table = NoiseTable.make(rate)
        assertEquals(rate, table.size)
        assertFalse(table.any { it < -1 || it >= 1 })
    }
}

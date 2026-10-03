package io.adambailey.framebeat.engine

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** `spec/voices.json` against [Voices] and [Envelope]. */
class SharedSpecVoiceTest {
    private val spec = SharedSpec.load("voices")
    private val tolerance = spec.double("tolerance")

    private fun voice(name: String): VoiceSpec = if (name == "ding") Voices.ding else Voices.forSound(SharedSpec.sound(name))

    private fun breakpoints(json: JsonArray): List<Pair<Double, Double>> = json.map {
        val point = it.jsonArray
        assertEquals(2, point.size)
        point[0].jsonPrimitive.double to point[1].jsonPrimitive.double
    }

    @Test
    fun voiceParameters() {
        val voices = spec.obj("voices")
        assertEquals(setOf("bass", "edge", "click", "ding"), voices.keys)
        for ((name, json) in voices) {
            val expected = json.jsonObject
            val actual = voice(name)
            val oscillators = expected.objects("oscillators")
            assertEquals(name, oscillators.size, actual.oscillators.size)
            for ((a, e) in actual.oscillators.zip(oscillators)) {
                assertEquals(name, e.string("waveform"), a.waveform.id)
                assertEquals("$name freq", breakpoints(e.array("freq")), a.freq.points)
                assertEquals("$name gain", breakpoints(e.array("gain")), a.gain.points)
            }
            val noises = expected.objects("noises")
            assertEquals(name, noises.size, actual.noises.size)
            for ((a, e) in actual.noises.zip(noises)) {
                assertEquals(name, e.string("filter"), a.filter.id)
                assertEquals(name, e.double("frequency"), a.frequency, 0.0)
                assertEquals(name, e.double("q"), a.q, 0.0)
                assertEquals("$name noise gain", breakpoints(e.array("gain")), a.gain.points)
            }
        }
    }

    @Test
    fun envelopeSamples() {
        val cases = spec.objects("envelopes")
        assertFalse(cases.isEmpty())
        for (c in cases) {
            val part = c.string("part")
            val parts = part.split(".")
            assertEquals(part, 3, parts.size)
            val index = parts[1].toInt()
            val voice = voice(c.string("voice"))
            val envelope = when (parts[0] to parts[2]) {
                "oscillators" to "freq" -> voice.oscillators[index].freq
                "oscillators" to "gain" -> voice.oscillators[index].gain
                "noises" to "gain" -> voice.noises[index].gain
                else -> error("unknown envelope part $part")
            }
            for (s in c.objects("samples")) {
                val t = s.double("t")
                assertEquals("${c.string("voice")} $part at $t", s.double("value"), envelope.value(t), tolerance)
            }
        }
    }
}

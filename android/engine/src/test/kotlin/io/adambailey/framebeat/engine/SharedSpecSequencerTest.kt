package io.adambailey.framebeat.engine

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** `spec/sequencer.json` against `Schedule.kt`. */
class SharedSpecSequencerTest {
    private val spec = SharedSpec.load("sequencer")
    private val tolerance = spec.double("tolerance")

    private fun rank(e: ScheduledEvent) = when {
        e is ScheduledEvent.Bell -> 0
        (e as ScheduledEvent.Step).line == LineId.Bottom -> 1
        else -> 2
    }

    @Test
    fun eventLists() {
        val sequences = spec.objects("sequences")
        assertFalse(sequences.isEmpty())
        for (c in sequences) {
            val name = c.string("name")
            val actual = generateEvents(
                top = SharedSpec.line(c.obj("top")),
                bottom = SharedSpec.line(c.obj("bottom")),
                bpm = c.double("bpm"),
                bars = c.int("bars"),
                chimeOnOne = c.bool("chimeOnOne"),
            )
            val expected = c.objects("events")
            assertEquals(name, expected.size, actual.size)
            for ((i, pair) in actual.zip(expected).withIndex()) {
                val (event, e) = pair
                val label = "$name, event $i"
                assertEquals(label, e.double("time"), event.time, tolerance)
                when (event) {
                    is ScheduledEvent.Bell -> assertEquals(label, "bell", e.string("kind"))
                    is ScheduledEvent.Step -> {
                        assertEquals(label, "step", e.string("kind"))
                        assertEquals(label, e.string("line"), if (event.line == LineId.Top) "top" else "bottom")
                        assertEquals(label, e.int("index"), event.index)
                        assertEquals(label, e.string("sound"), event.sound.id)
                        assertEquals(label, e.bool("audible"), event.audible)
                    }
                }
                if (i > 0) {
                    val prev = actual[i - 1]
                    val tie = abs(event.time - prev.time) <= tolerance
                    assertTrue(label, if (tie) rank(event) > rank(prev) else event.time > prev.time)
                }
            }
        }
    }

    @Test
    fun nextBarBoundary() {
        val cases = spec.objects("nextBarBoundary")
        assertFalse(cases.isEmpty())
        for (c in cases) {
            assertEquals(
                "anchor ${c.double("anchor")}, bar ${c.double("bar")}, not before ${c.double("notBefore")}",
                c.double("expected"),
                nextBarBoundary(c.double("anchor"), c.double("bar"), c.double("notBefore")),
                0.0,
            )
        }
    }
}

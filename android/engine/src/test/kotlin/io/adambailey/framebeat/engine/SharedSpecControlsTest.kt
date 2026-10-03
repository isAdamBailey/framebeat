package io.adambailey.framebeat.engine

import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** `spec/controls.json` against `Controls.kt`. */
class SharedSpecControlsTest {
    private val spec = SharedSpec.load("controls")

    @Test
    fun defaults() {
        val d = spec.obj("defaults")
        assertEquals(d.int("bpm"), Defaults.BPM)
        assertEquals(SharedSpec.line(d.obj("top")), Defaults.top)
        assertEquals(SharedSpec.line(d.obj("bottom")), Defaults.bottom)
        assertEquals(Defaults.top, Defaults.line(LineId.Top))
        assertEquals(Defaults.bottom, Defaults.line(LineId.Bottom))
        assertEquals(d.bool("chimeOnOne"), Defaults.CHIME_ON_ONE)
    }

    @Test
    fun ranges() {
        val r = spec.obj("ranges")
        val bpm = r.obj("bpm")
        val count = r.obj("count")
        assertEquals(bpm.int("min")..bpm.int("max"), Ranges.bpm)
        assertEquals(count.int("min")..count.int("max"), Ranges.count)
        assertEquals(count.int("max"), MAX_STEPS)
    }

    @Test
    fun drumKeys() {
        val expected = spec.obj("drumKeys").mapValues { (_, v) ->
            val o = v.jsonObject
            DrumKey(SharedSpec.side(o.string("side")), SharedSpec.sound(o.string("sound")))
        }
        assertEquals(expected, DRUM_KEYS)
    }

    @Test
    fun instrumentShortcuts() {
        for (c in spec.objects("instrumentShortcuts")) {
            val actual = instrumentShortcut(
                key = c.string("key"),
                repeat = c.bool("repeat"),
                meta = c.bool("metaKey"),
                ctrl = c.bool("ctrlKey"),
                alt = c.bool("altKey"),
            )
            val expected = when (val e = c.stringOrNull("expected")) {
                null -> null
                "playPause" -> InstrumentShortcut.PlayPause
                "bell" -> InstrumentShortcut.Bell
                else -> error("unknown shortcut $e")
            }
            assertEquals(c.toString(), expected, actual)
        }
    }

    @Test
    fun linesOutsideTheRangesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { Line(count = 0, sound = Sound.Bass) }
        assertThrows(IllegalArgumentException::class.java) { Line(count = MAX_STEPS + 1, sound = Sound.Bass) }
        assertThrows(IllegalArgumentException::class.java) { Line(count = 4, sound = Sound.Bass, dots = List(4) { true }) }
    }
}

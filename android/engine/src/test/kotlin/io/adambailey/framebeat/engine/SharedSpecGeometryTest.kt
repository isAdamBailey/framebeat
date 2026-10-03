package io.adambailey.framebeat.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** `spec/geometry.json` against [DrumGeometry]. */
class SharedSpecGeometryTest {
    private val spec = SharedSpec.load("geometry")
    private val tolerance = spec.double("tolerance")
    private val swingTolerance = spec.double("swingTolerance")

    @Test
    fun classify() {
        val cases = spec.objects("classify")
        assertFalse(cases.isEmpty())
        for (c in cases) {
            val note = c.string("note")
            val hit = DrumGeometry.classify(c.double("x"), c.double("y"))
            val expected = c.objOrNull("expected")
            if (expected == null) {
                assertNull(note, hit)
                continue
            }
            assertNotNull(note, hit)
            hit!!
            assertEquals(note, SharedSpec.sound(expected.string("sound")), hit.sound)
            assertEquals(note, SharedSpec.side(expected.string("side")), hit.side)
            assertEquals(note, expected.double("dx"), hit.dx, tolerance)
            assertEquals(note, expected.double("dy"), hit.dy, tolerance)
        }
    }

    @Test
    fun zonePoint() {
        val cases = spec.objects("zonePoint")
        assertEquals(Sound.entries.size * Side.entries.size, cases.size)
        for (c in cases) {
            val label = "${c.string("sound")} ${c.string("side")}"
            val point = DrumGeometry.zonePoint(SharedSpec.sound(c.string("sound")), SharedSpec.side(c.string("side")))
            assertEquals(label, c.double("x"), point.x, tolerance)
            assertEquals(label, c.double("y"), point.y, tolerance)
        }
    }

    @Test
    fun swing() {
        val cases = spec.objects("swing")
        assertFalse(cases.isEmpty())
        for (c in cases) {
            val label = "${c.string("side")} to (${c.double("gx")}, ${c.double("gy")})"
            val swing = DrumGeometry.swing(SharedSpec.side(c.string("side")), c.double("gx"), c.double("gy"))
            assertEquals(label, c.double("rotation"), swing.rotation, tolerance)
            assertEquals(label, c.double("x"), swing.x, swingTolerance)
            assertEquals(label, c.double("y"), swing.y, swingTolerance)
        }
    }
}

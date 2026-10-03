package io.adambailey.framebeat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The sizes DESIGN.md's "Phone and Tablet (native)" section writes down. */
class StageLayoutTest {
    @Test
    fun compactDrumTakesTheRowFirst() {
        // Window width to drum width, from DESIGN.md.
        for ((window, drum) in listOf(320f to 224f, 360f to 264f, 390f to 294f, 412f to 316f, 436f to 340f)) {
            val layout = StageLayout.of(window)
            assertFalse(layout.expanded)
            assertEquals("drum at $window", drum, layout.drumWidth, 0.001f)
            assertEquals("bell at $window", 0.5f, layout.bellScale, 0.001f)
        }
    }

    @Test
    fun compactBellTakesTheWidthPastTheDrumCap() {
        assertEquals(340f, StageLayout.of(450f).drumWidth, 0.001f)
        assertEquals((450f - 32f - 8f - 340f) / 112f, StageLayout.of(450f).bellScale, 0.001f)
        assertEquals(0.94f, StageLayout.of(599f).bellScale, 0.001f)
    }

    @Test
    fun compactRowNeverOverflows() {
        for (window in 200..599) {
            val layout = StageLayout.of(window.toFloat())
            val row = layout.drumWidth + layout.gap + layout.bellScale * StageLayout.BELL_WIDTH
            assertTrue("row at $window", row <= window - 2 * layout.gutter + 0.001f)
        }
    }

    @Test
    fun compactIgnoresHeight() {
        assertEquals(1f, StageLayout.of(412f, stageRoom = 10f).stageScale)
    }

    @Test
    fun expandedIsTheIpadStage() {
        val layout = StageLayout.of(600f)
        assertTrue(layout.expanded)
        assertEquals(340f, layout.drumWidth, 0.001f)
        assertEquals(1.625f, layout.bellScale, 0.001f)
        assertEquals(24f, layout.gap)
        assertEquals(24f, layout.gutter)
        // The row fits at the breakpoint: 340 + 24 + 182 = 546, plus 48 of gutters.
        assertTrue(layout.drumWidth + layout.gap + layout.bellScale * StageLayout.BELL_WIDTH + 2 * layout.gutter <= 600f)
    }

    @Test
    fun shortExpandedWindowScalesTheStageToAFloor() {
        val half = StageLayout.of(1280f, stageRoom = 340f * 30f / 32f / 2)
        assertEquals(0.5f, half.stageScale, 0.001f)
        assertEquals(170f, half.drumWidth, 0.001f)
        assertEquals(1.625f * 0.5f, half.bellScale, 0.001f)
        assertEquals(0.45f, StageLayout.of(1280f, stageRoom = 0f).stageScale)
        assertEquals(1f, StageLayout.of(1280f, stageRoom = 1000f).stageScale)
    }
}

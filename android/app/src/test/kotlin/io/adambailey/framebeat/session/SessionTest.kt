package io.adambailey.framebeat.session

import io.adambailey.framebeat.engine.Defaults
import io.adambailey.framebeat.engine.LineId
import io.adambailey.framebeat.engine.Pattern
import io.adambailey.framebeat.engine.Sound
import io.adambailey.framebeat.engine.Strike
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    @Test
    fun startsFromTheDefaults() {
        val session = Session()
        assertEquals(Pattern(Defaults.BPM, Defaults.top, Defaults.bottom, Defaults.CHIME_ON_ONE), session.pattern)
        assertNull(session.strikes.top)
        assertNull(session.strikes.bottom)
        assertEquals(0, session.bellTrigger)
    }

    @Test
    fun bpmAndCountsStayInRange() {
        val session = Session()
        session.changeBpm(10)
        assertEquals(40, session.bpm)
        session.changeBpm(500)
        assertEquals(200, session.bpm)
        session.changeCount(LineId.Top, 0)
        assertEquals(1, session.top.count)
        session.changeCount(LineId.Bottom, 17)
        assertEquals(16, session.bottom.count)
    }

    @Test
    fun editsChangeOnlyTheirLine() {
        val session = Session()
        session.toggleDot(LineId.Top, 2)
        session.changeSound(LineId.Top, Sound.Click)
        session.changeMuted(LineId.Top, true)
        assertFalse(session.top.dots[2])
        assertEquals(15, session.top.dots.count { it })
        assertEquals(Sound.Click, session.top.sound)
        assertTrue(session.top.muted)
        assertEquals(Defaults.bottom, session.bottom)
        session.toggleDot(LineId.Top, 2)
        assertTrue(session.top.dots[2])
    }

    @Test
    fun strikesCountUpPerLine() {
        val session = Session()
        session.recordStrike(Sound.Bass, LineId.Bottom)
        session.recordStrike(Sound.Bass, LineId.Bottom)
        session.recordStrike(Sound.Edge, LineId.Top)
        assertEquals(Strike(Sound.Bass, LineId.Bottom, 2), session.strikes.bottom)
        assertEquals(Strike(Sound.Edge, LineId.Top, 1), session.strikes.top)
    }

    @Test
    fun theChimeAndBellAreTheirOwnControls() {
        val session = Session()
        session.chimeOnOne = false
        assertFalse(session.pattern.chimeOnOne)
        session.ringBell()
        session.ringBell()
        assertEquals(2, session.bellTrigger)
    }
}

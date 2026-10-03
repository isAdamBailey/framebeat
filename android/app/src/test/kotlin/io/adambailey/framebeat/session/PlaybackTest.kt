package io.adambailey.framebeat.session

import io.adambailey.framebeat.engine.LineId
import io.adambailey.framebeat.engine.LiveScheduler
import io.adambailey.framebeat.engine.ScheduledEvent
import io.adambailey.framebeat.engine.Sound
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackTest {
    private val session = Session()
    private val booked = mutableListOf<ScheduledEvent>()

    /** The first bar's start, in virtual milliseconds after Play. */
    private val startMs = (LiveScheduler.START_DELAY * 1000).toLong()

    /** A playback whose clock is the test's virtual time, ticking in the background. */
    private fun TestScope.playback() = Playback(
        session,
        backgroundScope,
        clock = { testScheduler.currentTime / 1000.0 },
        book = { booked += it },
        latency = { 0.0 },
        // Tick on the test's virtual clock, not a real thread.
        tickContext = EmptyCoroutineContext,
    )

    @Test
    fun startsStoppedAndStopClearsThePlayhead() = runTest {
        val playback = playback()
        assertFalse(playback.playing)
        playback.toggle()
        assertTrue(playback.playing)
        advanceTimeBy(startMs)
        playback.frame()
        playback.toggle()
        assertFalse(playback.playing)
        assertNull(playback.currentTop)
        assertNull(playback.currentBottom)
        assertEquals(0.0, playback.progress, 0.0)
    }

    @Test
    fun theOneIsHeardOnBothLinesWithTheChime() = runTest {
        val playback = playback()
        playback.start()
        advanceTimeBy(startMs - 1)
        playback.frame()
        assertNull(playback.currentBottom)
        assertNull(session.strikes.bottom)
        advanceTimeBy(1)
        playback.frame()
        assertEquals(0, playback.currentTop)
        assertEquals(0, playback.currentBottom)
        assertEquals(Sound.Edge, session.strikes.top?.sound)
        assertEquals(Sound.Bass, session.strikes.bottom?.sound)
        assertEquals(1, session.bellTrigger)
    }

    @Test
    fun chimeOffRingsNoBell() = runTest {
        session.chimeOnOne = false
        val playback = playback()
        playback.start()
        advanceTimeBy(3000)
        playback.frame()
        assertEquals(0, session.bellTrigger)
        assertTrue(booked.none { it is ScheduledEvent.Bell })
    }

    @Test
    fun aMutedLineMovesThePlayheadButDoesNotStrike() = runTest {
        session.changeMuted(LineId.Bottom, true)
        val playback = playback()
        playback.start()
        advanceTimeBy(startMs)
        playback.frame()
        assertEquals(0, playback.currentBottom)
        assertNull(session.strikes.bottom)
        assertEquals(Sound.Edge, session.strikes.top?.sound)
    }

    @Test
    fun ticksBookAheadUntilStopped() = runTest {
        val playback = playback()
        playback.start()
        // 90 BPM: bottom steps every 2/3 s from the first bar, booked LOOK_AHEAD ahead.
        advanceTimeBy(2000)
        runCurrent()
        val bottomSteps = booked.filterIsInstance<ScheduledEvent.Step>().filter { it.line == LineId.Bottom }
        val first = LiveScheduler.START_DELAY
        assertEquals(listOf(first, first + 2.0 / 3, first + 4.0 / 3, first + 2.0), bottomSteps.map { it.time })
        assertTrue(bottomSteps.last().time < 2.0 + LiveScheduler.LOOK_AHEAD)
        playback.stop()
        val count = booked.size
        advanceTimeBy(5000)
        assertEquals(count, booked.size)
    }
}

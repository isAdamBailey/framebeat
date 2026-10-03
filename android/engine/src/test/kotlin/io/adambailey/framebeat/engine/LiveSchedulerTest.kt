package io.adambailey.framebeat.engine

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The live scheduler on a fake clock: steady play, re-anchoring, and the frame sync. */
class LiveSchedulerTest {
    private val tolerance = 1e-9
    private val start = LiveScheduler.START_DELAY
    private val tick = LiveScheduler.TICK_MS / 1000.0

    private var now = 0.0
    private val booked = mutableListOf<ScheduledEvent>()
    private val scheduler = LiveScheduler(clock = { now }, book = { booked += it })

    private val default = Pattern(Defaults.BPM, Defaults.top, Defaults.bottom, Defaults.CHIME_ON_ONE)

    /** Ticks every 25 ms from the current time until [until], under [pattern] at each tick's time. */
    private fun play(until: Double, pattern: (Double) -> Pattern) {
        while (now < until) {
            scheduler.tick(pattern(now))
            now += tick
        }
    }

    /** The last horizon booked: everything before it should be in [booked]. */
    private val horizon get() = now - tick + LiveScheduler.LOOK_AHEAD

    /** [pattern]'s offline events, moved to start at [from] and kept before [until]. */
    private fun segment(pattern: Pattern, from: Double, until: Double): List<ScheduledEvent> {
        val bars = ((until - from) / pattern.shape.barDuration).toInt() + 2
        return generateEvents(pattern.top, pattern.bottom, pattern.bpm.toDouble(), bars, pattern.chimeOnOne)
            .map { it.shifted(from) }
            .filter { it.time < until - tolerance }
    }

    private fun ScheduledEvent.shifted(by: Double) = when (this) {
        is ScheduledEvent.Bell -> copy(time = time + by)
        is ScheduledEvent.Step -> copy(time = time + by)
    }

    private fun ScheduledEvent.untimed() = when (this) {
        is ScheduledEvent.Bell -> copy(time = 0.0)
        is ScheduledEvent.Step -> copy(time = 0.0)
    }

    private fun assertBooked(expected: List<ScheduledEvent>) {
        assertEquals("event count", expected.size, booked.size)
        for ((i, pair) in expected.zip(booked).withIndex()) {
            val (e, a) = pair
            assertEquals("event $i time", e.time, a.time, tolerance)
            assertEquals("event $i", e.untimed(), a.untimed())
        }
        for (i in 1 until booked.size) {
            if (booked[i].time < booked[i - 1].time - tolerance) fail("event $i books before event ${i - 1}")
        }
    }

    /** The first old-pattern bar boundary at or after the horizon of the first tick at or after [changeAt]. */
    private fun boundaryAfter(changeAt: Double, anchor: Double, bar: Double): Double {
        var t = 0.0
        while (t < changeAt) t += tick
        return nextBarBoundary(anchor, bar, t + LiveScheduler.LOOK_AHEAD)
    }

    @Test
    fun steadyPlayBooksTheOfflineEvents() {
        scheduler.start(default)
        play(10.0) { default }
        assertBooked(segment(default, start, horizon))
    }

    @Test
    fun aBpmChangeFinishesTheBarThenRestartsBothLinesOnTheBoundary() {
        val faster = default.copy(bpm = 140)
        val changeAt = 1.0
        scheduler.start(default)
        play(9.0) { if (it < changeAt) default else faster }
        val b = boundaryAfter(changeAt, start, default.shape.barDuration)
        assertEquals(start + default.shape.barDuration, b, tolerance)
        assertBooked(segment(default, start, b) + segment(faster, b, horizon))
        val atBoundary = booked.filter { abs(it.time - b) < tolerance }
        assertTrue(atBoundary[0] is ScheduledEvent.Bell)
        assertEquals(listOf(LineId.Bottom to 0, LineId.Top to 0), atBoundary.drop(1).map { (it as ScheduledEvent.Step).line to it.index })
    }

    @Test
    fun aCountChangeRestartsOnTheNextBar() {
        val uneven = default.copy(top = default.top.copy(count = 7), bottom = default.bottom.copy(count = 5))
        val changeAt = 4.0
        scheduler.start(default)
        play(14.0) { if (it < changeAt) default else uneven }
        val b = boundaryAfter(changeAt, start, default.shape.barDuration)
        assertBooked(segment(default, start, b) + segment(uneven, b, horizon))
    }

    @Test
    fun aChangeWhoseHorizonCrossesABoundaryWaitsForTheNextOne() {
        val bar = default.shape.barDuration
        // The first tick whose horizon passes the end of bar 1, but whose clock has not.
        var t = 0.0
        while (t + LiveScheduler.LOOK_AHEAD <= start + bar) t += tick
        assertTrue(t < start + bar)
        val slower = default.copy(bpm = 60)
        scheduler.start(default)
        play(12.0) { if (it < t - tolerance) default else slower }
        val b = boundaryAfter(t - tolerance, start, bar)
        assertEquals(start + 2 * bar, b, tolerance)
        assertBooked(segment(default, start, b) + segment(slower, b, horizon))
    }

    @Test
    fun severalChangesInOneBarRestartOnceUnderTheLatest() {
        val first = default.copy(bpm = 120)
        val last = default.copy(bpm = 150, top = default.top.copy(count = 5))
        scheduler.start(default)
        play(9.0) {
            when {
                it < 0.5 -> default
                it < 1.5 -> first
                else -> last
            }
        }
        val b = boundaryAfter(0.5, start, default.shape.barDuration)
        assertBooked(segment(default, start, b) + segment(last, b, horizon))
    }

    @Test
    fun changingBackBeforeTheBoundaryKeepsTheTiming() {
        scheduler.start(default)
        play(6.0) { if (it in 1.0..1.5) default.copy(bpm = 100) else default }
        assertBooked(segment(default, start, horizon))
    }

    @Test
    fun randomChangesNeverDropDoubleOrReorder() {
        val random = Random(16)
        var pattern = default
        val changes = (1..40).map { it * 1.7 }
        scheduler.start(default)
        var next = 0
        play(70.0) {
            if (next < changes.size && it >= changes[next]) {
                next++
                pattern = pattern.copy(
                    bpm = random.nextInt(Ranges.bpm.first, Ranges.bpm.last + 1),
                    top = pattern.top.copy(count = random.nextInt(1, MAX_STEPS + 1)),
                    bottom = pattern.bottom.copy(count = random.nextInt(1, MAX_STEPS + 1)),
                )
            }
            pattern
        }
        for (i in 1 until booked.size) {
            val (a, b) = booked[i - 1] to booked[i]
            assertTrue("event $i is out of order", b.time >= a.time - tolerance)
            if (abs(b.time - a.time) < tolerance) assertFalse("event $i is doubled", a == b || a.untimed() == b.untimed())
        }
        // Every bar starts with the chime and both lines' first step, together.
        val bells = booked.filterIsInstance<ScheduledEvent.Bell>()
        assertTrue(bells.size > 10)
        for (bell in bells) {
            val together = booked.filter { it is ScheduledEvent.Step && abs(it.time - bell.time) < tolerance && it.index == 0 }
            assertEquals("lines at ${bell.time}", 2, together.size)
        }
    }

    @Test
    fun muteAndDotsChangeWhatSoundsButNotWhen() {
        val muted = default.copy(top = default.top.copy(muted = true))
        scheduler.start(default)
        play(5.0) { if (it < 2.0) default else muted }
        val expected = segment(default, start, horizon)
        assertEquals(expected.size, booked.size)
        for ((e, a) in expected.zip(booked)) assertEquals(e.time, a.time, tolerance)
        val tops = booked.filterIsInstance<ScheduledEvent.Step>().filter { it.line == LineId.Top }
        assertTrue(tops.filter { it.time < 2.0 }.all { it.audible })
        assertTrue(tops.filter { it.time > 2.0 + LiveScheduler.LOOK_AHEAD }.none { it.audible })
    }

    @Test
    fun turningTheChimeOffStopsTheBellFromTheNextUnbookedBar() {
        scheduler.start(default)
        play(4.0) { if (it < 1.0) default else default.copy(chimeOnOne = false) }
        assertEquals(listOf(start), booked.filterIsInstance<ScheduledEvent.Bell>().map { it.time })
    }

    @Test
    fun stopAndStartAgainAnchorsAfreshAndBooksNothingWhileStopped() {
        scheduler.start(default)
        play(1.0) { default }
        scheduler.stop()
        val before = booked.size
        play(2.0) { default }
        assertEquals(before, booked.size)
        val restart = now
        booked.clear()
        scheduler.start(default)
        play(6.0) { default }
        assertBooked(segment(default, restart + start, horizon))
    }

    @Test
    fun frameFiresStepsAndTheChimeWhenTheyAreHeard() {
        val pattern = default.copy(top = default.top.copy(muted = true))
        val heard = mutableListOf<Pair<Sound, LineId>>()
        var dings = 0
        scheduler.start(pattern)
        assertEquals(Playhead(null, null, 0.0), scheduler.frame(pattern, onStep = { s, l -> heard += s to l }))

        // Just before the one, with 20 ms of output latency still to go.
        now = start - 0.01
        scheduler.tick(pattern)
        assertEquals(Playhead(null, null, 0.0), scheduler.frame(pattern, latency = 0.0))
        val one = scheduler.frame(pattern, latency = 0.02, onStep = { s, l -> heard += s to l }, onDing = { dings++ })
        assertEquals(0, one.top)
        assertEquals(0, one.bottom)
        // Heard 10 ms into a 4-beat bar.
        assertEquals(0.01 / pattern.shape.barDuration, one.progress, 1e-9)
        assertEquals(1, dings)
        // The muted top line moves its playhead but does not strike.
        assertEquals(listOf(Sound.Bass to LineId.Bottom), heard)

        // Halfway through beat 2 of 4.
        val beat = pattern.shape.beat
        now = start + 1.5 * beat
        scheduler.tick(pattern)
        val mid = scheduler.frame(pattern)
        assertEquals(1, mid.bottom)
        assertEquals(1.5 / 4, mid.progress, 1e-9)
        assertEquals(1, mid.top)

        scheduler.stop()
        assertEquals(Playhead.Stopped, scheduler.frame(pattern))
    }

    @Test
    fun theMixerPlaysWhatTheSchedulerBooks() {
        val rate = 48_000
        val mixer = Mixer(rate, NoiseTable.make(rate, Random(3)))
        val live = LiveScheduler(clock = { mixer.seconds }, book = mixer::book)
        val block = FloatArray(256)
        val out = FloatArray(3 * rate)
        live.start(default)
        var offset = 0
        var lastTick = -1.0
        while (offset < out.size) {
            if (mixer.seconds - lastTick >= tick) {
                live.tick(default)
                lastTick = mixer.seconds
            }
            mixer.render(block)
            block.copyInto(out, offset, 0, minOf(block.size, out.size - offset))
            offset += block.size
        }
        val offline = Mixer.renderOffline(
            rate,
            out.size,
            segment(default, start, 3.0).filter { it !is ScheduledEvent.Step || it.audible }.map {
                Math.round(it.time * rate) to if (it is ScheduledEvent.Step) Voices.forSound(it.sound) else Voices.ding
            },
            NoiseTable.make(rate, Random(3)),
        )
        assertArrayEquals(offline, out, 0f)
        assertEquals(0f, out.take((start * rate).toInt()).maxOf { abs(it) })
    }
}

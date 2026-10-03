package io.adambailey.framebeat.engine

import kotlin.math.ceil
import kotlin.math.max

// Port of src/lib/schedule.ts: the sequencer's timing as pure functions,
// shared by LiveScheduler and the spec/ tests. Times are seconds.

/**
 * The bar's timing. Held fixed from one anchor to the next, so a BPM or
 * step-count change can't reshape a bar that is already partly booked.
 */
data class Shape(val beat: Double, val bottomCount: Int, val topCount: Int) {
    val barDuration: Double get() = beat * bottomCount
}

fun shapeFor(bpm: Double, bottomCount: Int, topCount: Int) = Shape(60 / bpm, bottomCount, topCount)

/** The number of whole bars from [anchor] to the first bar boundary at or after [notBefore]. */
fun barsUntil(anchor: Double, barDuration: Double, notBefore: Double): Int =
    max(0.0, ceil((notBefore - anchor) / barDuration)).toInt()

/** The first bar boundary (anchor + k bars, k >= 0) at or after [notBefore]. */
fun nextBarBoundary(anchor: Double, barDuration: Double, notBefore: Double): Double =
    anchor + barsUntil(anchor, barDuration, notBefore) * barDuration

/**
 * Two step streams counted from one anchor. Steps are timed as
 * anchor + index * duration rather than by adding onto a running total, so
 * bar boundaries land exactly where a re-anchor expects.
 */
class Streams(var shape: Shape, var anchorTime: Double, var bIdx: Int = 0, var tIdx: Int = 0) {
    fun bottomStepTime(i: Int) = anchorTime + i * shape.beat

    fun topStepTime(i: Int) = anchorTime + i * shape.barDuration / shape.topCount
}

/**
 * Books every step before [horizon], in time order, stopping each line at its
 * index limit. On a tie the bottom line goes first. [onStep] gets the line,
 * the step index within the bar, and the time.
 */
inline fun bookSteps(
    s: Streams,
    horizon: Double,
    bLimit: Int = Int.MAX_VALUE,
    tLimit: Int = Int.MAX_VALUE,
    onStep: (line: LineId, index: Int, time: Double) -> Unit,
) {
    while (true) {
        val bt = if (s.bIdx < bLimit) s.bottomStepTime(s.bIdx) else Double.POSITIVE_INFINITY
        val tt = if (s.tIdx < tLimit) s.topStepTime(s.tIdx) else Double.POSITIVE_INFINITY
        if (bt >= horizon && tt >= horizon) return
        if (bt <= tt) {
            onStep(LineId.Bottom, s.bIdx % s.shape.bottomCount, bt)
            s.bIdx++
        } else {
            onStep(LineId.Top, s.tIdx % s.shape.topCount, tt)
            s.tIdx++
        }
    }
}

sealed interface ScheduledEvent {
    val time: Double

    data class Bell(override val time: Double) : ScheduledEvent

    data class Step(
        override val time: Double,
        val line: LineId,
        val index: Int,
        val sound: Sound,
        val audible: Boolean,
    ) : ScheduledEvent
}

/**
 * What one booked step produces. The "one" is when both lines restart
 * together (bottom step 0): the bar-marker chime rings there, unless it is
 * switched off, regardless of either line's mute.
 */
fun stepEvents(line: LineId, index: Int, time: Double, data: Line, chimeOnOne: Boolean): List<ScheduledEvent> {
    val step = ScheduledEvent.Step(time, line, index, data.sound, audible = data.dots[index] && !data.muted)
    return if (line == LineId.Bottom && index == 0 && chimeOnOne) listOf(ScheduledEvent.Bell(time), step) else listOf(step)
}

/**
 * Every event of [bars] whole bars from time 0, booked the way the live
 * scheduler books them. The offline equivalent of the live path.
 */
fun generateEvents(top: Line, bottom: Line, bpm: Double, bars: Int, chimeOnOne: Boolean): List<ScheduledEvent> {
    val s = Streams(shapeFor(bpm, bottom.count, top.count), anchorTime = 0.0)
    val events = mutableListOf<ScheduledEvent>()
    bookSteps(s, Double.POSITIVE_INFINITY, bars * bottom.count, bars * top.count) { line, index, time ->
        events += stepEvents(line, index, time, if (line == LineId.Top) top else bottom, chimeOnOne)
    }
    return events
}

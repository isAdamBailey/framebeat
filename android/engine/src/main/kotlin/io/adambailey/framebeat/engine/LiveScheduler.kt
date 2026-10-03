package io.adambailey.framebeat.engine

import kotlin.math.roundToLong

// Port of src/composables/useSequencer.ts: the look-ahead scheduler and the
// frame sync, with the audio clock injected so the re-anchor is unit-tested.

/** Everything the sequencer reads, as of one tick or frame. */
data class Pattern(val bpm: Int, val top: Line, val bottom: Line, val chimeOnOne: Boolean) {
    fun line(id: LineId) = if (id == LineId.Top) top else bottom

    val shape: Shape get() = shapeFor(bpm.toDouble(), bottom.count, top.count)
}

/** The step each line is on, as heard, and how far through the bar the bottom line is (0 until 1). */
data class Playhead(val top: Int?, val bottom: Int?, val progress: Double) {
    companion object {
        val Stopped = Playhead(null, null, 0.0)
    }
}

/**
 * Drift-free polyrhythm scheduler: two independent step streams booked on the
 * audio clock. The bottom line runs at the BPM and defines the bar; the top
 * line divides the same bar into its own step count, so both lines always
 * start together on "one".
 *
 * Call [tick] every [TICK_MS] while playing, and [frame] once per display
 * frame. [clock] is the audio clock in seconds; [book] receives every booked
 * event, audible or not, in time order (see [Mixer.book]).
 *
 * Not thread-safe: call [start], [stop], [tick], and [frame] from one thread,
 * as the web calls them from its one event loop.
 */
class LiveScheduler(
    private val clock: () -> Double,
    private val book: (ScheduledEvent) -> Unit,
) {
    private sealed interface Heard {
        val time: Double

        class Bell(override val time: Double) : Heard

        class Top(override val time: Double, val index: Int) : Heard

        class Bottom(override val time: Double, val index: Int, val shape: Shape) : Heard
    }

    var playing = false
        private set

    private var streams = Streams(shapeFor(60.0, 1, 1), anchorTime = 0.0)
    private val queue = ArrayDeque<Heard>()
    private var lastBottom = Heard.Bottom(0.0, 0, streams.shape)
    private var currentTop: Int? = null
    private var currentBottom: Int? = null

    /** Restarts both step streams together on a fresh bar boundary at [startTime], under [pattern]'s BPM and counts. */
    private fun anchor(startTime: Double, pattern: Pattern) {
        streams = Streams(pattern.shape, startTime)
    }

    private fun scheduleStep(line: LineId, index: Int, time: Double, pattern: Pattern) {
        for (event in stepEvents(line, index, time, pattern.line(line), pattern.chimeOnOne)) {
            book(event)
            // A visual event per booked event, so the animation fires when it is heard.
            queue += when {
                event is ScheduledEvent.Bell -> Heard.Bell(time)
                line == LineId.Bottom -> Heard.Bottom(time, index, streams.shape)
                else -> Heard.Top(time, index)
            }
        }
    }

    /** Starts playback. Does nothing while already playing, as the web only starts from a stop. */
    fun start(pattern: Pattern) {
        if (playing) return
        anchor(clock() + START_DELAY, pattern)
        lastBottom = Heard.Bottom(streams.anchorTime, 0, streams.shape)
        queue.clear()
        playing = true
        tick(pattern)
    }

    fun stop() {
        playing = false
        queue.clear()
        currentTop = null
        currentBottom = null
    }

    /**
     * Books every step before the look-ahead horizon. When [pattern]'s BPM or
     * either count differs from the bar being booked, finishes the old pattern
     * up to its next bar boundary at or after this horizon, then restarts both
     * lines there, so nothing booked is dropped, doubled, or overlapped. The web
     * sets the same re-anchor from a watcher; here the tick compares shapes.
     */
    fun tick(pattern: Pattern) {
        if (!playing) return
        val horizon = clock() + LOOK_AHEAD
        if (pattern.shape != streams.shape) {
            val anchorTime = streams.anchorTime
            val shape = streams.shape
            val bars = barsUntil(anchorTime, shape.barDuration, horizon)
            bookSteps(streams, Double.POSITIVE_INFINITY, bars * shape.bottomCount, bars * shape.topCount) { line, i, t ->
                scheduleStep(line, i, t, pattern)
            }
            anchor(nextBarBoundary(anchorTime, shape.barDuration, horizon), pattern)
        }
        bookSteps(streams, horizon) { line, i, t -> scheduleStep(line, i, t, pattern) }
    }

    /**
     * Drains the events already heard and returns the playhead. A sound booked
     * at time T is heard at T + the output [latency], so the visuals compare
     * against that. [onStep] fires for a step whose dot is on and whose line is
     * not muted, read from [pattern] now; [onDing] fires for the chime.
     */
    fun frame(
        pattern: Pattern,
        latency: Double = 0.0,
        onStep: (Sound, LineId) -> Unit = { _, _ -> },
        onDing: () -> Unit = {},
    ): Playhead {
        if (!playing) return Playhead.Stopped
        val now = clock() + latency
        fun heard(line: LineId, index: Int) {
            val data = pattern.line(line)
            if (data.dots[index] && !data.muted) onStep(data.sound, line)
        }
        while (queue.isNotEmpty() && queue.first().time <= now) {
            when (val event = queue.removeFirst()) {
                is Heard.Bell -> onDing()
                is Heard.Bottom -> {
                    lastBottom = event
                    currentBottom = event.index
                    heard(LineId.Bottom, event.index)
                }
                is Heard.Top -> {
                    currentTop = event.index
                    heard(LineId.Top, event.index)
                }
            }
        }
        // Use the shape of the step being heard, which can lag the one being booked.
        val shape = lastBottom.shape
        val frac = ((now - lastBottom.time) / shape.beat).coerceIn(0.0, 1.0)
        val progress = ((lastBottom.index + frac) % shape.bottomCount) / shape.bottomCount
        return Playhead(currentTop, currentBottom, progress)
    }

    companion object {
        /** How far past the clock each tick books, in seconds. */
        const val LOOK_AHEAD = 0.12

        /** How long after Play the first bar starts, in seconds. */
        const val START_DELAY = 0.1

        /** How often to call [tick]: the web's `setInterval` period. */
        const val TICK_MS = 25L
    }
}

/** The mixer's clock in seconds, for [LiveScheduler]. */
val Mixer.seconds: Double get() = frame.toDouble() / sampleRate

/** Plays a booked event: the bell, or an audible step, at the nearest frame. Used live and by [renderEvents]. */
fun Mixer.book(event: ScheduledEvent) {
    val atFrame = (event.time * sampleRate).roundToLong()
    when (event) {
        is ScheduledEvent.Bell -> triggerDing(atFrame)
        is ScheduledEvent.Step -> if (event.audible) trigger(event.sound, atFrame)
    }
}

/** Renders [frames] frames of [events] offline, through the same [Mixer.book] the live path uses. */
fun renderEvents(
    sampleRate: Int,
    frames: Int,
    events: List<ScheduledEvent>,
    noise: DoubleArray = NoiseTable.make(sampleRate),
): FloatArray {
    val mixer = Mixer(sampleRate, noise)
    events.forEach(mixer::book)
    return FloatArray(frames).also { mixer.render(it) }
}

package io.adambailey.framebeat.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.adambailey.framebeat.engine.LineId
import io.adambailey.framebeat.engine.LiveScheduler
import io.adambailey.framebeat.engine.Playhead
import io.adambailey.framebeat.engine.ScheduledEvent
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The sequencer as the screen sees it: port of `useSequencer`'s state and
 * loops around :engine's [LiveScheduler]. Compose observes [playing],
 * [currentTop], [currentBottom], and [progress], each on its own, so the
 * playhead moving every frame redraws the playhead and nothing else.
 *
 * Play books against [clock] (the mixer's, in seconds) through [book], ticking
 * in [scope] on [tickContext] every [LiveScheduler.TICK_MS]. Call [frame]
 * once per display frame while playing: it lights the steps being heard,
 * [latency] seconds behind the clock, and hands their strikes and chimes to
 * [session].
 *
 * Threads: call [start], [stop], and [frame] from the main thread. The ticks
 * run off it, so a stalled main thread (a rotation, a long layout) cannot
 * starve the look-ahead and leave steps late; a lock keeps them and [frame]
 * from using the scheduler at once.
 */
class Playback(
    private val session: Session,
    private val scope: CoroutineScope,
    clock: () -> Double,
    book: (ScheduledEvent) -> Unit,
    private val latency: () -> Double,
    private val tickContext: CoroutineContext = Dispatchers.Default,
) {
    private val scheduler = LiveScheduler(clock, book)
    private var ticker: Job? = null
    private val onStep = session::recordStrike
    private val onDing = session::recordBell

    var playing by mutableStateOf(false)
        private set

    /** The step each line is on as heard, or null while stopped. */
    var currentTop by mutableStateOf<Int?>(null)
        private set
    var currentBottom by mutableStateOf<Int?>(null)
        private set

    /** How far through the bar the bottom line is, 0 until 1. */
    var progress by mutableDoubleStateOf(0.0)
        private set

    fun toggle() = if (playing) stop() else start()

    /** The step [id]'s line is on as heard, or null while stopped. */
    fun current(id: LineId): Int? = if (id == LineId.Top) currentTop else currentBottom

    fun start() {
        if (playing) return
        synchronized(scheduler) { scheduler.start(session.pattern) }
        playing = true
        ticker = scope.launch(tickContext) {
            while (isActive) {
                delay(LiveScheduler.TICK_MS)
                synchronized(scheduler) { scheduler.tick(session.pattern) }
            }
        }
    }

    /** Stops booking. Steps already booked still play, as on the web. */
    fun stop() {
        ticker?.cancel()
        ticker = null
        // A tick already under way finishes first, then finds the scheduler stopped.
        synchronized(scheduler) { scheduler.stop() }
        playing = false
        show(Playhead.Stopped)
    }

    fun frame() {
        if (!playing) return
        val latency = latency()
        show(synchronized(scheduler) { scheduler.frame(session.pattern, latency, onStep, onDing) })
    }

    private fun show(playhead: Playhead) {
        currentTop = playhead.top
        currentBottom = playhead.bottom
        progress = playhead.progress
    }
}

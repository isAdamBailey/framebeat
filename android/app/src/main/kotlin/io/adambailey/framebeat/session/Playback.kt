package io.adambailey.framebeat.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.adambailey.framebeat.engine.LiveScheduler
import io.adambailey.framebeat.engine.ScheduledEvent
import kotlinx.coroutines.CoroutineScope
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
 * in [scope] every [LiveScheduler.TICK_MS]. Call [frame] once per display
 * frame while playing: it lights the steps being heard, [latency] seconds
 * behind the clock, and hands their strikes and chimes to [session].
 *
 * Threads: everything here, and the ticks in [scope], run on the main thread.
 */
class Playback(
    private val session: Session,
    private val scope: CoroutineScope,
    clock: () -> Double,
    book: (ScheduledEvent) -> Unit,
    private val latency: () -> Double,
) {
    private val scheduler = LiveScheduler(clock, book)
    private var ticker: Job? = null

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

    fun start() {
        if (playing) return
        scheduler.start(session.pattern)
        playing = true
        ticker = scope.launch {
            while (isActive) {
                delay(LiveScheduler.TICK_MS)
                scheduler.tick(session.pattern)
            }
        }
    }

    /** Stops booking. Steps already booked still play, as on the web. */
    fun stop() {
        ticker?.cancel()
        ticker = null
        scheduler.stop()
        playing = false
        currentTop = null
        currentBottom = null
        progress = 0.0
    }

    fun frame() {
        if (!playing) return
        val playhead = scheduler.frame(
            session.pattern,
            latency(),
            onStep = session::recordStrike,
            onDing = session::ringBell,
        )
        currentTop = playhead.top
        currentBottom = playhead.bottom
        progress = playhead.progress
    }
}

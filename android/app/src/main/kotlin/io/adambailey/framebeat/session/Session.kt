package io.adambailey.framebeat.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.adambailey.framebeat.engine.Defaults
import io.adambailey.framebeat.engine.Line
import io.adambailey.framebeat.engine.LineId
import io.adambailey.framebeat.engine.Pattern
import io.adambailey.framebeat.engine.Ranges
import io.adambailey.framebeat.engine.Sound
import io.adambailey.framebeat.engine.Strike
import io.adambailey.framebeat.engine.Strikes

/**
 * The instrument's state: port of HomeView.vue's refs and AppState.swift.
 * Compose observes every field. Held in memory for the session and never
 * saved, so a new process starts from [Defaults].
 *
 * Whether the sequencer is playing, and where its playhead is, belongs to the
 * sequencer, as `useSequencer` owns them on the web.
 */
class Session {
    var bpm by mutableIntStateOf(Defaults.BPM)
        private set
    var top by mutableStateOf(Defaults.top)
        private set
    var bottom by mutableStateOf(Defaults.bottom)
        private set

    /** Whether the sequencer rings the bell on "the one". */
    var chimeOnOne by mutableStateOf(Defaults.CHIME_ON_ONE)

    /** The latest sequencer strike per line, for the drum to animate. */
    var strikes by mutableStateOf(Strikes())
        private set

    /** Bumps each time the bell rings, by hand or on the one. */
    var bellTrigger by mutableIntStateOf(0)
        private set

    fun line(id: LineId): Line = if (id == LineId.Top) top else bottom

    /** Everything the sequencer reads, as of now. */
    val pattern: Pattern get() = Pattern(bpm, top, bottom, chimeOnOne)

    fun changeBpm(value: Int) {
        bpm = value.coerceIn(Ranges.bpm)
    }

    fun toggleDot(id: LineId, index: Int) = patch(id) { line ->
        line.copy(dots = line.dots.mapIndexed { i, on -> if (i == index) !on else on })
    }

    fun changeCount(id: LineId, count: Int) = patch(id) { it.copy(count = count.coerceIn(Ranges.count)) }

    fun changeSound(id: LineId, sound: Sound) = patch(id) { it.copy(sound = sound) }

    fun changeMuted(id: LineId, muted: Boolean) = patch(id) { it.copy(muted = muted) }

    fun recordStrike(sound: Sound, id: LineId) {
        strikes = strikes.with(Strike(sound, id, (strikes[id]?.id ?: 0) + 1))
    }

    fun ringBell() {
        bellTrigger++
    }

    private fun patch(id: LineId, change: (Line) -> Line) {
        if (id == LineId.Top) top = change(top) else bottom = change(bottom)
    }
}

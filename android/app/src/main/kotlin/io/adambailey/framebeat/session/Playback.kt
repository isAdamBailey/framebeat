package io.adambailey.framebeat.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.adambailey.framebeat.engine.Playhead

/**
 * Whether the sequencer is playing, and where its playhead is: the state
 * `useSequencer` owns on the web. Compose observes both.
 *
 * Play and Pause only flip [playing] for now; the sequencer is not wired to it yet.
 */
class Playback {
    var playing by mutableStateOf(false)
        private set

    var playhead by mutableStateOf(Playhead.Stopped)
        private set

    fun toggle() {
        playing = !playing
    }
}

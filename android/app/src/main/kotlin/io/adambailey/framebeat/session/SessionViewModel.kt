package io.adambailey.framebeat.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.adambailey.framebeat.audio.AudioOutput
import io.adambailey.framebeat.engine.Sound

/**
 * Keeps the [session] and the [output] across rotation and other
 * configuration changes. It has no saved state, so neither outlives the process.
 */
class SessionViewModel(application: Application) : AndroidViewModel(application) {
    val session = Session()
    val output = AudioOutput(application)
    val playback = Playback()

    /** Plays a hand strike on the drum now. The drum animates it. */
    fun strike(sound: Sound) = output.play(sound)

    /**
     * Struck by hand: rings now, whether or not the sequencer's chime on the
     * one is switched on, as `HomeView.vue`'s `ringBell` does.
     */
    fun ringBell() {
        output.playDing()
        session.ringBell()
    }

    override fun onCleared() {
        output.stop()
    }
}

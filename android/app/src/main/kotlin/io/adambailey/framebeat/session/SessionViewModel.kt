package io.adambailey.framebeat.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.adambailey.framebeat.audio.AudioOutput
import io.adambailey.framebeat.engine.Sound
import io.adambailey.framebeat.engine.book
import io.adambailey.framebeat.engine.seconds

/**
 * Keeps the [session], the [output], and [playback] across rotation and other
 * configuration changes, so the sequencer keeps ticking through a rotation.
 * It has no saved state, so none of them outlives the process.
 */
class SessionViewModel(application: Application) : AndroidViewModel(application) {
    val session = Session()
    val output = AudioOutput(application)
    val playback = Playback(
        session,
        viewModelScope,
        clock = { output.mixer.seconds },
        book = output.mixer::book,
        latency = { output.latency },
    )

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
        playback.stop()
        output.stop()
    }
}

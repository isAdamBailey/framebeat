package io.adambailey.framebeat.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.adambailey.framebeat.audio.AudioOutput
import io.adambailey.framebeat.engine.InstrumentShortcut
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
        session.recordBell()
    }

    /** Space or B on the focused drum or bell, as `HomeView.vue`'s `handleShortcut` does. */
    fun shortcut(shortcut: InstrumentShortcut) = when (shortcut) {
        InstrumentShortcut.PlayPause -> playback.toggle()
        InstrumentShortcut.Bell -> ringBell()
    }

    /** Stops the sequencer and the audio: the app has left the screen, or is gone. */
    fun pause() {
        playback.stop()
        output.stop()
    }

    override fun onCleared() {
        pause()
    }
}

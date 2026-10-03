package io.adambailey.framebeat.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.adambailey.framebeat.audio.AudioOutput

/**
 * Keeps the [session] and the [output] across rotation and other
 * configuration changes. It has no saved state, so neither outlives the process.
 */
class SessionViewModel(application: Application) : AndroidViewModel(application) {
    val session = Session()
    val output = AudioOutput(application)

    override fun onCleared() {
        output.stop()
    }
}

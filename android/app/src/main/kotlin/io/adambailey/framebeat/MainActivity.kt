package io.adambailey.framebeat

import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.adambailey.framebeat.session.SessionViewModel
import io.adambailey.framebeat.ui.StageScreen

class MainActivity : ComponentActivity() {
    private val model: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // The volume keys set the media volume the drum plays at, even between hits.
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent {
            StageScreen(
                bellTrigger = model.session.bellTrigger,
                onStrike = model::strike,
                onRingBell = model::ringBell,
            )
        }
    }

    // Audio runs only while the app is on screen, as on iPad: no background
    // playback and no foreground service. A rotation keeps it running.
    override fun onStart() {
        super.onStart()
        model.output.start()
    }

    override fun onStop() {
        if (!isChangingConfigurations) model.output.stop()
        super.onStop()
    }
}

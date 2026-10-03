package io.adambailey.framebeat

import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.adambailey.framebeat.ui.Fonts
import io.adambailey.framebeat.session.SessionViewModel
import io.adambailey.framebeat.ui.Palette

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
        setContent { StageScreen() }
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

@Composable
private fun StageScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Stage),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "FrameBeat",
            style = TextStyle(
                color = Palette.Title,
                fontFamily = Fonts.Display,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                letterSpacing = (-0.025).em,
            ),
        )
    }
}

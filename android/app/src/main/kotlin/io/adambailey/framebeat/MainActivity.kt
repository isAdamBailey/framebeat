package io.adambailey.framebeat

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import io.adambailey.framebeat.ui.Palette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { StageScreen() }
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

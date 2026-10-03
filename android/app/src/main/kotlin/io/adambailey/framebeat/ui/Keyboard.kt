package io.adambailey.framebeat.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import io.adambailey.framebeat.engine.InstrumentShortcut
import io.adambailey.framebeat.engine.instrumentShortcut

// Hardware keys for the drum and the bell, read through :engine's key map
// (DRUM_KEYS and instrumentShortcut) the way DrumCanvas.vue and HomeView.vue read them.

/**
 * This key as the web's `KeyboardEvent.key`, lowercased: the arrows by name,
 * Space as " ", and anything else as the character it types, so a letter
 * follows the keyboard's layout as it does in a browser.
 */
internal fun KeyEvent.webKey(): String? = when (key) {
    Key.DirectionLeft -> "arrowleft"
    Key.DirectionRight -> "arrowright"
    Key.Spacebar -> " "
    else -> utf16CodePoint.takeIf { it > 0 }?.let { String(Character.toChars(it)).lowercase() }
}

/**
 * Space and B on a focused instrument, the drum or the bell: Space plays or
 * pauses through [onShortcut], B rings the bell. Key repeat and Ctrl, Alt, or
 * Meta chords are ignored, as [instrumentShortcut] says; a held Space or B is
 * swallowed rather than passed on to whatever holds the instrument. Any other
 * key down goes to [onKey], which returns whether it used it.
 */
internal fun Modifier.instrumentKeys(
    onShortcut: (InstrumentShortcut) -> Unit,
    onKey: (KeyEvent, String) -> Boolean = { _, _ -> false },
): Modifier = onKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
    val name = event.webKey() ?: return@onKeyEvent false
    val shortcut = instrumentShortcut(
        name,
        repeat = event.nativeKeyEvent.repeatCount > 0,
        meta = event.isMetaPressed,
        ctrl = event.isCtrlPressed,
        alt = event.isAltPressed,
    )
    when {
        shortcut != null -> {
            onShortcut(shortcut)
            true
        }
        // A repeat of Space or B, chord-free: still ours, so it does nothing.
        (name == " " || name == "b") && !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed -> true
        else -> onKey(event, name)
    }
}

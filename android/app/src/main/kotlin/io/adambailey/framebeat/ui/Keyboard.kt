package io.adambailey.framebeat.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.InputMode
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
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
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

/** Whether this key down is an auto-repeat of a held key. */
internal val KeyEvent.isRepeat: Boolean get() = nativeKeyEvent.repeatCount > 0

/** Key downs only, by their web name; [onKey] returns whether it used the key. */
internal fun Modifier.onKeyDown(onKey: (KeyEvent, String) -> Boolean): Modifier = onKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
    val name = event.webKey() ?: return@onKeyEvent false
    onKey(event, name)
}

/**
 * Space and B for everything inside: put it on the one element holding the
 * drum and the bell, HomeView.vue's `data-instrument` check, so the shortcuts
 * work only while one of them has focus. Space plays or pauses through
 * [onShortcut]; B rings the bell. Ctrl, Alt, and Meta chords pass through;
 * a held Space or B is swallowed without repeating, as [instrumentShortcut] says.
 */
internal fun Modifier.instrumentShortcuts(onShortcut: (InstrumentShortcut) -> Unit): Modifier = onKeyDown { event, name ->
    val shortcut = instrumentShortcut(
        name,
        repeat = false,
        meta = event.isMetaPressed,
        ctrl = event.isCtrlPressed,
        alt = event.isAltPressed,
    ) ?: return@onKeyDown false
    if (!event.isRepeat) onShortcut(shortcut)
    true
}

/**
 * Whether the element focused through [interaction] should show its focus
 * ring: only while the keyboard is in use, as `:focus-visible` does on the
 * web. Read it while drawing, so a change only redraws.
 */
@Composable
internal fun rememberFocusVisible(interaction: InteractionSource): () -> Boolean {
    val focused = interaction.collectIsFocusedAsState()
    val modes = LocalInputModeManager.current
    return remember(focused, modes) { { focused.value && modes.inputMode == InputMode.Keyboard } }
}

/**
 * What makes the drum and the bell instruments for the keyboard: focusable,
 * by Tab or through [focus], with the sky focus ring ([ring], 4dp out, the
 * web's `ring-offset-4`) shown while [focusVisible], and their own key downs
 * handed to [onKey]. Call [focus]'s `requestFocus` on a press too, so a click
 * focuses it as it does a web `<button>`.
 */
internal fun Modifier.instrument(
    focus: FocusRequester,
    interaction: MutableInteractionSource,
    focusVisible: () -> Boolean,
    ring: Shape,
    onKey: (KeyEvent, String) -> Boolean,
): Modifier = outerRing(ring, Palette.BassSky, gap = 4.dp, show = focusVisible)
    .focusRequester(focus)
    .onKeyDown(onKey)
    .focusable(interactionSource = interaction)

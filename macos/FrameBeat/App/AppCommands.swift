import Foundation

/// Menu-bar → view bridge. `FrameBeatApp`'s `Commands` scene has no access
/// to `ContentView`'s local playback/engine state (the sequencer and audio
/// engine live there, not at the App level), so menu actions post a
/// notification and `ContentView` reacts — the smallest wiring that gets
/// real menu-bar shortcuts (⌘↑/⌘↓ tempo, ⌘M mute, Reset Pattern) without
/// hoisting the whole playback stack.
extension Notification.Name {
    static let fbTogglePlay = Notification.Name("io.adambailey.framebeat.togglePlay")
    static let fbTempoUp = Notification.Name("io.adambailey.framebeat.tempoUp")
    static let fbTempoDown = Notification.Name("io.adambailey.framebeat.tempoDown")
    static let fbToggleMute = Notification.Name("io.adambailey.framebeat.toggleMute")
    static let fbResetPattern = Notification.Name("io.adambailey.framebeat.resetPattern")
}

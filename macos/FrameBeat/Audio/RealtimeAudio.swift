import FrameBeatCore

/// Thin demo-app wrapper around `LiveAudioEngine` (the real Phase 3
/// real-time engine, now implemented in FrameBeatCore) — kept as its own
/// type so `ContentView`/`DrumView`/`DemoSequencer` didn't need to change
/// when this stopped being a pre-render/player-node-pool shortcut and
/// became the real thing.
public final class RealtimeAudio {
    /// Exposed so `LiveSequencer` (Phase 4) can trigger against the exact
    /// same engine instance/clock that direct drum taps use — otherwise the
    /// sequencer's `now` and a tap's `now` would be two unrelated clocks.
    public let engine: LiveAudioEngine

    public init() {
        engine = LiveAudioEngine()
        try? engine.start()
    }

    public func play(_ sound: Sound) {
        engine.trigger(sound, atSample: engine.now)
    }

    public func playDing() {
        engine.triggerDing(atSample: engine.now)
    }
}

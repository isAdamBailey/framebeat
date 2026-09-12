import FrameBeatCore

/// Thin app-side wrapper around `LiveAudioEngine` (the real Phase 3
/// real-time engine, implemented in FrameBeatCore).
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

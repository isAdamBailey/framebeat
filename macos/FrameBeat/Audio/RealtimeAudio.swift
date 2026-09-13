import FrameBeatCore
#if os(iOS)
import AVFoundation
#endif

/// Thin app-side wrapper around `LiveAudioEngine` (the real Phase 3
/// real-time engine, implemented in FrameBeatCore).
public final class RealtimeAudio {
    /// Exposed so `LiveSequencer` (Phase 4) can trigger against the exact
    /// same engine instance/clock that direct drum taps use — otherwise the
    /// sequencer's `now` and a tap's `now` would be two unrelated clocks.
    public let engine: LiveAudioEngine

    public init() {
        #if os(iOS)
        // macOS has no session concept — audio output just works. iOS is
        // silent (or worse, obeys the physical mute switch) without an
        // active `.playback` session; unlike the Mac target this has no
        // local way to verify short of running on an iOS
        // device/simulator and actually listening.
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default)
        try? session.setActive(true)
        #endif
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

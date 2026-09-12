import Foundation

/// A shared 1-second white-noise table generator — matches drumAudio.ts's
/// module-level `noiseBuffer` (one `AudioBuffer`, read from index 0 on every
/// strike) in spirit: both `OfflineRenderer` and `LiveAudioEngine` use one
/// table per instance rather than a fresh RNG draw per voice.
public enum NoiseTable {
    public static func make(sampleRate: Double) -> [Double] {
        var rng = SystemRandomNumberGenerator()
        let n = Int(sampleRate)
        return (0..<n).map { _ in Double.random(in: -1...1, using: &rng) }
    }
}

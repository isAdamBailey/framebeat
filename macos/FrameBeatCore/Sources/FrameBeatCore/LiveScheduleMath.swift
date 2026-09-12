import Foundation

/// The sample-domain equivalent of the "multiply from the anchor, don't
/// accumulate" fix already applied to the offline `Sequencer` — see that
/// file's doc comment for why accumulation (`next += stepDuration`
/// repeatedly) drifts. `LiveSequencer` calls this once per due step rather
/// than generating a whole batch up front, but the formula is identical:
/// each step's sample time is computed fresh from the anchor and an
/// integer step index, never by adding onto a running total.
public enum LiveScheduleMath {
    public static func sampleTime(anchorSample: Int64, stepIndex: Int, stepDurationSeconds: Double, sampleRate: Double) -> Int64 {
        anchorSample + Int64((Double(stepIndex) * stepDurationSeconds * sampleRate).rounded())
    }

    /// The smallest bar boundary (`anchorSample + k * barDurationSamples`,
    /// `k >= 0`) that is not before `notBefore`. Used by `LiveSequencer` to
    /// re-anchor on a bpm/step-count change: a re-anchor can't take effect
    /// any sooner than the samples already committed to the audio engine
    /// (everything up to the tick's own look-ahead horizon), and it must
    /// land on an old-pattern bar boundary rather than mid-bar, or the
    /// current bar gets truncated and old/new-pattern events collide.
    public static func nextBarBoundary(anchorSample: Int64, barDurationSamples: Int64, notBefore: Int64) -> Int64 {
        precondition(barDurationSamples > 0)
        let delta = notBefore - anchorSample
        if delta <= 0 { return anchorSample }
        let steps = (delta + barDurationSamples - 1) / barDurationSamples
        return anchorSample + steps * barDurationSamples
    }
}

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
}

import Foundation

/// Models the subset of Web Audio `AudioParam` automation used by
/// `src/lib/drumAudio.ts`: an initial `setValueAtTime` jump followed by one
/// or more `exponentialRampToValueAtTime` segments. Used for both gain
/// envelopes and oscillator frequency glides (e.g. the open-bass 100→58 Hz
/// pitch drop).
///
/// Web Audio's exponential ramp formula for t in [t0, t1]:
///   value(t) = v0 * (v1 / v0) ^ ((t - t0) / (t1 - t0))
/// which is why every gain envelope in the original bottoms out at 0.0001
/// rather than 0 — the ramp is undefined/infinite at v == 0.
public struct Envelope: Sendable {
    public let points: [(time: Double, value: Double)]

    public init(_ points: [(time: Double, value: Double)]) {
        precondition(!points.isEmpty, "Envelope needs at least one breakpoint")
        self.points = points
    }

    public func value(at t: Double) -> Double {
        var prev = points[0]
        if t <= prev.time { return prev.value }
        for p in points.dropFirst() {
            if t <= p.time {
                if p.time == prev.time { return p.value }
                let frac = (t - prev.time) / (p.time - prev.time)
                if prev.value <= 0 || p.value <= 0 {
                    // Shouldn't occur given the 0.0001 floors in the original,
                    // but fall back to linear rather than producing NaN.
                    return prev.value + (p.value - prev.value) * frac
                }
                return prev.value * pow(p.value / prev.value, frac)
            }
            prev = p
        }
        return points[points.count - 1].value
    }

    public var duration: Double { points[points.count - 1].time }
}

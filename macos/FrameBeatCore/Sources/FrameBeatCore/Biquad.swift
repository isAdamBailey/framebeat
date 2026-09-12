import Foundation

/// A single second-order IIR biquad filter, matching the Web Audio spec's
/// `BiquadFilterNode` coefficient formulas for lowpass/highpass/bandpass.
///
/// **Q is not interpreted the same way for every type** — this was verified
/// empirically (not from spec text, which only came back truncated during
/// the port) by comparing impulse responses against a real
/// `OfflineAudioContext` in Chrome:
///   - `lowpass`/`highpass`: Q is in **decibels** — `alpha = sin(w0) / (2 ·
///     10^(Q/20))`. At the default Q of 1 that's `10^0.05 ≈ 1.122`, not
///     `1.0`; using linear Q here measured an RMS impulse-response error of
///     ~0.01 against Chrome (vs. ~1e-9, i.e. floating-point noise, with the
///     dB conversion).
///   - `bandpass`: Q is **linear** — `alpha = sin(w0) / (2Q)` — confirmed the
///     same way (dB conversion was the one that mismatched, by the same
///     ~0.01 RMS).
///
/// Each `BiquadFilter` instance corresponds to one `createBiquadFilter()`
/// call in the original — a fresh, zero-state filter per drum strike.
public struct BiquadFilter {
    public enum Kind {
        case lowpass
        case highpass
        case bandpass
    }

    private let b0, b1, b2, a1, a2: Double
    private var x1: Double = 0
    private var x2: Double = 0
    private var y1: Double = 0
    private var y2: Double = 0

    public init(kind: Kind, frequency: Double, q: Double, sampleRate: Double) {
        let w0 = 2 * Double.pi * frequency / sampleRate
        let cosW0 = cos(w0)
        let sinW0 = sin(w0)
        let effectiveQ: Double
        switch kind {
        case .lowpass, .highpass:
            effectiveQ = pow(10, q / 20) // Q in dB for these two types.
        case .bandpass:
            effectiveQ = q // Q used linearly.
        }
        let alpha = sinW0 / (2 * effectiveQ)

        var rb0 = 0.0, rb1 = 0.0, rb2 = 0.0, ra0 = 0.0, ra1 = 0.0, ra2 = 0.0
        switch kind {
        case .lowpass:
            rb0 = (1 - cosW0) / 2
            rb1 = 1 - cosW0
            rb2 = (1 - cosW0) / 2
            ra0 = 1 + alpha
            ra1 = -2 * cosW0
            ra2 = 1 - alpha
        case .highpass:
            rb0 = (1 + cosW0) / 2
            rb1 = -(1 + cosW0)
            rb2 = (1 + cosW0) / 2
            ra0 = 1 + alpha
            ra1 = -2 * cosW0
            ra2 = 1 - alpha
        case .bandpass:
            // Constant 0 dB peak-gain variant (matches Web Audio's bandpass).
            rb0 = alpha
            rb1 = 0
            rb2 = -alpha
            ra0 = 1 + alpha
            ra1 = -2 * cosW0
            ra2 = 1 - alpha
        }
        b0 = rb0 / ra0
        b1 = rb1 / ra0
        b2 = rb2 / ra0
        a1 = ra1 / ra0
        a2 = ra2 / ra0
    }

    public mutating func process(_ x0: Double) -> Double {
        let y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x0
        y2 = y1
        y1 = y0
        return y0
    }
}

import FrameBeatCore
import Foundation

// Stand-in for the XCTest suite in Tests/FrameBeatCoreTests. This CLT-only
// environment's `swift test` fails with a broken SwiftPM manifest linker
// unrelated to this package (a trivial one-target manifest fails
// identically) — see ../../README.md "Known build issue". Until a real
// Xcode is installed, this executable target is the actual regression
// check that runs; it's part of the package (not a /tmp scratch file) so it
// survives and travels with the repo. Re-run the real XCTest suite once
// `swift test` works and consider retiring this once that's confirmed.

var failures = 0
func check(_ name: String, _ pass: Bool) {
    print(pass ? "ok   \(name)" : "FAIL \(name)")
    if !pass { failures += 1 }
}

// --- Envelope: Web Audio exponential-ramp formula -----------------------
let env = Envelope([(0, 0.0001), (0.008, 0.9)])
let t = 0.004
let expected = 0.0001 * pow(0.9 / 0.0001, (t - 0) / (0.008 - 0))
check("exponential ramp matches Web Audio formula", abs(env.value(at: t) - expected) < 1e-12)
check("envelope holds first value before start", Envelope([(0.01, 5), (0.02, 10)]).value(at: 0) == 5)
check("envelope holds last value after end", Envelope([(0, 1), (0.1, 2)]).value(at: 1) == 2)

// --- Biquad: golden impulse-response samples captured from a real ------
// `OfflineAudioContext` in Chrome (frequency/Q matching the values
// drumAudio.ts actually uses), to pin the Q-is-in-dB-for-lowpass/highpass-
// but-linear-for-bandpass behavior documented in Biquad.swift.
func biquadImpulse(kind: BiquadFilter.Kind, freq: Double, q: Double, n: Int) -> [Double] {
    var f = BiquadFilter(kind: kind, frequency: freq, q: q, sampleRate: 48000)
    return (0..<n).map { f.process($0 == 0 ? 1.0 : 0.0) }
}
func rms(_ a: [Double], _ b: [Double]) -> Double {
    sqrt(zip(a, b).map { ($0 - $1) * ($0 - $1) }.reduce(0, +) / Double(a.count))
}
let chromeLowpass350Q1: [Double] = [
    0.0005141656147316098, 0.002035037614405155, 0.00400505168363452, 0.005888024810701609,
    0.007683565840125084, 0.009391479194164276, 0.011011757887899876, 0.012544575147330761,
    0.013990276493132114, 0.0153493732213974, 0.01662253588438034, 0.017810583114624023,
]
let chromeHighpass2200Q1: [Double] = [
    0.8693775534629822, -0.2588995397090912, -0.24535776674747467, -0.21692118048667908,
    -0.17901542782783508, -0.13653935492038727, -0.09362519532442093, -0.05350873991847038,
    -0.018494194373488426, 0.010004965588450432, 0.031369179487228394, 0.04563971608877182,
]
let chromeBandpass2600Q1_4: [Double] = [
    0.10651800781488419, 0.17942599952220917, 0.11189322918653488, 0.04727858304977417,
    -0.008416756056249142, -0.05138428509235382, -0.07993142306804657, -0.09420420974493027,
    -0.09578067064285278, -0.08720400929450989, -0.07151627540588379, -0.05184035748243332,
]
check(
    "lowpass(350, Q=1) impulse response matches Chrome within 1e-6 RMS",
    rms(biquadImpulse(kind: .lowpass, freq: 350, q: 1, n: 12), chromeLowpass350Q1) < 1e-6
)
check(
    "highpass(2200, Q=1) impulse response matches Chrome within 1e-6 RMS",
    rms(biquadImpulse(kind: .highpass, freq: 2200, q: 1, n: 12), chromeHighpass2200Q1) < 1e-6
)
check(
    "bandpass(2600, Q=1.4) impulse response matches Chrome within 1e-6 RMS",
    rms(biquadImpulse(kind: .bandpass, freq: 2600, q: 1.4, n: 12), chromeBandpass2600Q1_4) < 1e-6
)

// --- Synth: no NaN/Inf, soft clip bounds output --------------------------
let r = OfflineRenderer(sampleRate: 48000, durationSeconds: 1)
for s in Sound.allCases { r.trigger(s, atSample: 0) }
r.triggerDing(atSample: 0)
let mono = r.finalizeMono()
check("no NaN/Inf samples", mono.allSatisfy { !$0.isNaN && !$0.isInfinite })
check("soft clip bounds output to [-1,1]", (mono.map(abs).max() ?? 0) <= 1.0)

// --- Sequencer: mute behavior, bar-boundary coincidence -------------------
var mutedBottom = Line(count: 4, sound: .bass)
mutedBottom.muted = true
let top = Line(count: 3, sound: .edge)
let events = Sequencer.generateEvents(top: top, bottom: mutedBottom, bpm: 100, bars: 2)
let bellCount = events.filter { if case .bell = $0.kind { return true }; return false }.count
check("bar chime rings even when bottom line is muted", bellCount == 2)
let bottomAudible = events.contains { e in
    if case .step(.bottom, _, _, let audible) = e.kind { return audible }
    return false
}
check("muted line's steps are never marked audible", !bottomAudible)

let unevenEvents = Sequencer.generateEvents(
    top: Line(count: 7, sound: .click), bottom: Line(count: 5, sound: .bass), bpm: 120, bars: 6
)
let bottomZero = Set(unevenEvents.compactMap { e -> Double? in
    if case .step(.bottom, 0, _, _) = e.kind { return e.time }; return nil
})
let topZero = Set(unevenEvents.compactMap { e -> Double? in
    if case .step(.top, 0, _, _) = e.kind { return e.time }; return nil
})
check("5-vs-7 polyrhythm: bottom/top step 0 coincide on every bar", bottomZero == topZero && bottomZero.count == 6)

print(failures == 0 ? "\nALL CHECKS PASSED" : "\n\(failures) CHECK(S) FAILED")
exit(failures == 0 ? 0 : 1)

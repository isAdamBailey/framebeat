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

// Swift 6's strict concurrency checking treats top-level `var`s in a `main`
// file as main-actor-isolated; `nonisolated(unsafe)` is safe here since this
// script is single-threaded and top-to-bottom.
nonisolated(unsafe) var failures = 0
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

// --- LiveAudioEngine's streaming voices vs. OfflineRenderer's bulk ------
// renderer: the ding (pure oscillators, no noise table involved) should
// match sample-for-sample, since both paths now build from the same
// VoiceSpec tables. This is the regression test that would catch the live
// path drifting from the already-verified offline math.
do {
    let sr = 48000.0
    let masterGain = 0.85
    let n = Int(1.2 * sr)

    let offline = OfflineRenderer(sampleRate: sr, durationSeconds: 1.2)
    offline.triggerDing(atSample: 0)
    let offlineSamples = offline.finalizeMono()

    let liveComponents = VoiceSpec.dingComponents().map { LiveOscillatorComponent(spec: $0, sampleRate: sr) }
    let liveVoice = ActiveVoice(startSample: 0, components: liveComponents, sampleRate: sr)
    var maxDiff = 0.0
    for i in 0..<n {
        let live = tanh(liveVoice.sample(atGlobalSample: Int64(i)) * masterGain)
        let diff = abs(live - Double(offlineSamples[i]))
        maxDiff = max(maxDiff, diff)
    }
    check("live-engine ding matches offline renderer within 1e-5 (max diff: \(String(format: "%.2e", maxDiff)))", maxDiff < 1e-5)
}

// --- LiveAudioEngine's noise-based voices: sanity only (each side draws --
// its own independent noise table, so exact parity isn't expected/possible
// here — see NoiseTable.swift's doc comment).
do {
    let sr = 48000.0
    let noiseTable = NoiseTable.make(sampleRate: sr)
    var allOK = true
    for sound in Sound.allCases {
        let spec = VoiceSpec.components(for: sound)
        var components: [LiveVoiceComponent] = spec.oscillators.map { LiveOscillatorComponent(spec: $0, sampleRate: sr) }
        components += spec.noises.map { LiveFilteredNoiseComponent(spec: $0, sampleRate: sr, noiseTable: noiseTable) }
        let voice = ActiveVoice(startSample: 0, components: components, sampleRate: sr)
        var peak = 0.0
        var sawNaN = false
        for i in 0..<Int(0.8 * sr) {
            let s = voice.sample(atGlobalSample: Int64(i))
            if s.isNaN || s.isInfinite { sawNaN = true }
            peak = max(peak, abs(s))
        }
        if peak <= 0 || sawNaN { allOK = false }
    }
    check("live-engine voices (bass/edge/click) produce bounded, non-NaN audio", allOK)
}

// --- LiveScheduleMath: the same "multiply from anchor" bar-lock invariant --
// as the offline Sequencer, but in the sample domain that LiveSequencer
// actually schedules in.
do {
    let sr = 48000.0
    let bpm = 90.0
    let topCount = 3, bottomCount = 4
    let beatDur = 60.0 / bpm
    let barDur = beatDur * Double(bottomCount)
    let topStepDur = barDur / Double(topCount)
    let anchorSample: Int64 = 12345 // arbitrary, non-zero anchor like a real re-anchor would produce

    var allBarsLock = true
    for bar in 0..<8 {
        let bottomZeroIdx = bar * bottomCount
        let topZeroIdx = bar * topCount
        let bSample = LiveScheduleMath.sampleTime(anchorSample: anchorSample, stepIndex: bottomZeroIdx, stepDurationSeconds: beatDur, sampleRate: sr)
        let tSample = LiveScheduleMath.sampleTime(anchorSample: anchorSample, stepIndex: topZeroIdx, stepDurationSeconds: topStepDur, sampleRate: sr)
        if bSample != tSample { allBarsLock = false }
    }
    check("LiveScheduleMath: bottom/top step 0 land on the identical sample every bar", allBarsLock)
}

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

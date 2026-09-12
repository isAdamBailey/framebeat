import FrameBeatCore
import Foundation

// Renders a FrameBeat pattern offline to a WAV file and prints timing-
// invariant checks, so the ported synth/scheduler can be verified without
// Xcode, a signed build, or a live audio device.
//
// Usage:
//   swift run frame-beat-render [--bpm N] [--top N] [--top-sound bass|edge|click]
//     [--bottom N] [--bottom-sound bass|edge|click] [--bars N] [--out path.wav]
//
// Defaults match src/App.vue's initial state: bpm 90, top 3/edge, bottom 4/bass.

func arg(_ name: String, default def: String) -> String {
    let args = CommandLine.arguments
    if let i = args.firstIndex(of: "--\(name)"), i + 1 < args.count { return args[i + 1] }
    return def
}

let sampleRate = 48000.0
let bpm = Double(arg("bpm", default: "90"))!
let topCount = Int(arg("top", default: "3"))!
let bottomCount = Int(arg("bottom", default: "4"))!
let topSound = Sound(rawValue: arg("top-sound", default: "edge"))!
let bottomSound = Sound(rawValue: arg("bottom-sound", default: "bass"))!
let bars = Int(arg("bars", default: "4"))!
let outPath = arg("out", default: "frame-beat-render-output.wav")

let top = Line(count: topCount, sound: topSound)
let bottom = Line(count: bottomCount, sound: bottomSound)

let events = Sequencer.generateEvents(top: top, bottom: bottom, bpm: bpm, bars: bars)
let duration = events.map(\.time).max() ?? 0

let renderer = OfflineRenderer(sampleRate: sampleRate, durationSeconds: duration + 1.5)

func sampleIndex(_ t: Double) -> Int { Int((t * sampleRate).rounded()) }

for event in events {
    let sample = sampleIndex(event.time)
    switch event.kind {
    case .bell:
        renderer.triggerDing(atSample: sample)
    case .step(_, _, let sound, let audible):
        if audible { renderer.trigger(sound, atSample: sample) }
    }
}

let url = URL(fileURLWithPath: outPath)
try WavWriter.write(samples: renderer.finalizeMono(), sampleRate: sampleRate, to: url)

print("Wrote \(outPath) — \(bpm) BPM, top \(topCount)/\(topSound.rawValue), bottom \(bottomCount)/\(bottomSound.rawValue), \(bars) bars")

// --- Timing-invariant checks -------------------------------------------
// The whole point of the two-line polyrhythm: bottom step 0 and top step 0
// must coincide on every bar boundary, and the bell must ring there too,
// regardless of top/bottom step counts.
let beatDur = 60.0 / bpm
let barDur = beatDur * Double(bottomCount)

var failures: [String] = []
let bellTimes = events.compactMap { e -> Double? in
    if case .bell = e.kind { return e.time }
    return nil
}
if bellTimes.count != bars {
    failures.append("expected \(bars) bell events, got \(bellTimes.count)")
}
for (k, t) in bellTimes.enumerated() {
    let expected = Double(k) * barDur
    if abs(t - expected) > 1e-9 {
        failures.append("bell #\(k) at \(t)s, expected \(expected)s")
    }
}
let bottomZeroTimes = events.compactMap { e -> Double? in
    if case .step(.bottom, 0, _, _) = e.kind { return e.time }
    return nil
}
let topZeroTimes = events.compactMap { e -> Double? in
    if case .step(.top, 0, _, _) = e.kind { return e.time }
    return nil
}
for t in bottomZeroTimes {
    let nearestTop = topZeroTimes.min(by: { abs($0 - t) < abs($1 - t) })
    guard let nearestTop, abs(nearestTop - t) < 1e-9 else {
        let nearestDescription = nearestTop != nil ? String(nearestTop!) : "none"
        failures.append("bottom step 0 at \(t)s has no coincident top step 0 (nearest: \(nearestDescription))")
        continue
    }
}

let peak = renderer.buffer.map { abs($0) }.max() ?? 0
print("Peak sample magnitude before soft-clip: \(String(format: "%.3f", peak))" + (peak > 1 ? "  (soft-clip engaged)" : ""))

if failures.isEmpty {
    print("PASS: \(bars) bar boundaries verified — bell + top/bottom step 0 all coincide exactly.")
} else {
    print("FAIL:")
    for f in failures { print("  - \(f)") }
    exit(1)
}

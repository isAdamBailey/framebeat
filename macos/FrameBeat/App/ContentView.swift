import SwiftUI
import FrameBeatCore

// Phase 1 scaffold content, carried over from FrameBeatCore's
// FrameBeatDemo package target (which stays in place as a lightweight,
// no-Xcode-build way to smoke-test engine/geometry changes). Uses the
// real Phase 3/4 engine end to end: LiveAudioEngine for sample-accurate
// playback and LiveSequencer for drift-free scheduling. NOT Phase 5's
// final UI — no DESIGN.md token layer or transport-panel styling pass yet.

struct ContentView: View {
    @State private var bpm: Double = 90
    @State private var currentTop: Int?
    @State private var currentBottom: Int?
    @State private var bellFlash = false
    @State private var topStrike: StrikeEvent?
    @State private var bottomStrike: StrikeEvent?
    @State private var strikeIdCounter = 0
    @State private var playing = false

    private let audio = RealtimeAudio()
    @State private var sequencer: LiveSequencer?

    private let topCount = 3
    private let bottomCount = 4

    var body: some View {
        VStack(spacing: 24) {
            VStack(spacing: 4) {
                Text("FrameBeat")
                    .font(.system(size: 30, weight: .semibold, design: .serif))
                Text("Click the drum, or press Play")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            DrumView(
                onStrike: { sound, _ in audio.play(sound) },
                topStrike: $topStrike,
                bottomStrike: $bottomStrike
            )
            .frame(width: 340)

            Circle()
                .fill(bellFlash ? Color.white : Color.white.opacity(0.15))
                .frame(width: 12, height: 12)
                .animation(.easeOut(duration: 0.25), value: bellFlash)

            VStack(spacing: 14) {
                HStack(spacing: 16) {
                    Button(playing ? "Pause" : "Play") {
                        togglePlay()
                    }
                    .keyboardShortcut(.space, modifiers: [])
                    .frame(width: 80)

                    Slider(value: $bpm, in: 40...200, step: 1)
                        .onChange(of: bpm) { _, newValue in
                            if playing {
                                sequencer?.update(top: Line(count: topCount, sound: .edge), bottom: Line(count: bottomCount, sound: .bass), bpm: newValue)
                            }
                        }
                    Text("\(Int(bpm)) BPM")
                        .font(.system(.body, design: .serif).weight(.bold))
                        .frame(width: 70, alignment: .trailing)
                }
                .frame(width: 360)

                stepRow(label: "Top (\(topCount), edge)", current: currentTop, count: topCount, color: soundColor(.edge))
                stepRow(label: "Bottom (\(bottomCount), bass)", current: currentBottom, count: bottomCount, color: soundColor(.bass))
            }
        }
        .padding(32)
        .frame(width: 460, height: 620)
        .background(Color(red: 0.008, green: 0.023, blue: 0.09))
        .foregroundStyle(.white)
        .preferredColorScheme(.dark)
    }

    private func stepRow(label: String, current: Int?, count: Int, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.system(size: 11, weight: .semibold))
                .tracking(1.2)
                .textCase(.uppercase)
                .foregroundStyle(.secondary)
            HStack(spacing: 8) {
                ForEach(0..<count, id: \.self) { i in
                    Circle()
                        .fill(i == current ? color : Color.white.opacity(0.12))
                        .frame(width: i == current ? 16 : 12, height: i == current ? 16 : 12)
                        .animation(.easeOut(duration: 0.08), value: current)
                }
            }
        }
        .frame(width: 360, alignment: .leading)
    }

    private func togglePlay() {
        if playing {
            sequencer?.stop()
            playing = false
            currentTop = nil
            currentBottom = nil
        } else {
            let top = Line(count: topCount, sound: .edge)
            let bottom = Line(count: bottomCount, sound: .bass)
            let seq = LiveSequencer(engine: audio.engine, top: top, bottom: bottom, bpm: bpm)
            seq.onIndexUpdate = { line, index in
                if line == .top { currentTop = index } else { currentBottom = index }
            }
            seq.onStrike = { line, _, sound in
                strikeIdCounter += 1
                if line == .top {
                    topStrike = StrikeEvent(sound: sound, id: strikeIdCounter)
                } else {
                    bottomStrike = StrikeEvent(sound: sound, id: strikeIdCounter)
                }
            }
            seq.onBell = {
                bellFlash = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.12) { bellFlash = false }
            }
            seq.start()
            sequencer = seq
            playing = true
        }
    }
}

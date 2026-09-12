import SwiftUI
import FrameBeatCore

// Phase 1 scaffold content, carried over from FrameBeatCore's
// FrameBeatDemo package target (which stays in place as a lightweight,
// no-Xcode-build way to smoke-test engine/geometry changes). Uses the
// real Phase 3/4 engine end to end: LiveAudioEngine for sample-accurate
// playback and LiveSequencer for drift-free scheduling, plus Phase 2's
// AppState for the shared model. NOT Phase 5's final UI — no DESIGN.md
// token layer or transport-panel styling pass yet.

struct ContentView: View {
    @State private var appState = AppState()
    @State private var currentTop: Int?
    @State private var currentBottom: Int?
    @State private var bellFlash = false
    @State private var topStrike: StrikeEvent?
    @State private var bottomStrike: StrikeEvent?
    @State private var playing = false

    private let audio = RealtimeAudio()
    @State private var sequencer: LiveSequencer?

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

                    Slider(value: $appState.bpm, in: 40...200, step: 1)
                        .onChange(of: appState.bpm) { _, _ in reanchorIfPlaying() }
                    Text("\(Int(appState.bpm)) BPM")
                        .font(.system(.body, design: .serif).weight(.bold))
                        .frame(width: 70, alignment: .trailing)
                }
                .frame(width: 360)

                stepRow(label: "Top (\(appState.top.count), \(appState.top.sound.rawValue))", current: currentTop, count: appState.top.count, color: soundColor(appState.top.sound))
                Stepper("Top steps: \(appState.top.count)", value: $appState.top.count, in: 1...16)
                    .onChange(of: appState.top.count) { _, _ in reanchorIfPlaying() }
                    .frame(width: 360, alignment: .leading)

                stepRow(label: "Bottom (\(appState.bottom.count), \(appState.bottom.sound.rawValue))", current: currentBottom, count: appState.bottom.count, color: soundColor(appState.bottom.sound))
                Stepper("Bottom steps: \(appState.bottom.count)", value: $appState.bottom.count, in: 1...16)
                    .onChange(of: appState.bottom.count) { _, _ in reanchorIfPlaying() }
                    .frame(width: 360, alignment: .leading)
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

    private func reanchorIfPlaying() {
        if playing {
            sequencer?.update(top: appState.top, bottom: appState.bottom, bpm: appState.bpm)
        }
    }

    private func togglePlay() {
        if playing {
            sequencer?.stop()
            playing = false
            currentTop = nil
            currentBottom = nil
        } else {
            let seq = LiveSequencer(engine: audio.engine, top: appState.top, bottom: appState.bottom, bpm: appState.bpm)
            seq.onIndexUpdate = { line, index in
                if line == .top { currentTop = index } else { currentBottom = index }
            }
            seq.onStrike = { line, _, sound in
                appState.recordStrike(sound, on: line)
                let strike = line == .top ? appState.strikes.top : appState.strikes.bottom
                if line == .top {
                    topStrike = strike.map { StrikeEvent(sound: $0.sound, id: $0.id) }
                } else {
                    bottomStrike = strike.map { StrikeEvent(sound: $0.sound, id: $0.id) }
                }
            }
            seq.onBell = {
                appState.ringBell()
                bellFlash = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.12) { bellFlash = false }
            }
            seq.start()
            sequencer = seq
            playing = true
        }
    }
}

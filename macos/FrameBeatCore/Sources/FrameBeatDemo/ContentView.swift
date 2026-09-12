import SwiftUI
import FrameBeatCore

// Minimal proof-of-life for the ported Swift engine — NOT Phase 5's final
// UI (no DESIGN.md token layer, no transport-panel styling pass yet), but
// DrumView now exercises the real geometry math end to end: tap the drum,
// the mallet swings to the exact zone and a ripple lands where you tapped.

struct ContentView: View {
    @State private var bpm: Double = 90
    @State private var currentTop: Int?
    @State private var currentBottom: Int?
    @State private var bellFlash = false
    @State private var topStrike: StrikeEvent?
    @State private var bottomStrike: StrikeEvent?
    @State private var strikeIdCounter = 0

    private let audio = RealtimeAudio()
    @State private var sequencer: DemoSequencer?

    private let topCount = 3
    private let bottomCount = 4

    var body: some View {
        VStack(spacing: 24) {
            VStack(spacing: 4) {
                Text("FrameBeat")
                    .font(.system(size: 30, weight: .semibold, design: .serif))
                Text("Swift engine demo — click the drum, or press Play")
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
                    Button(sequencer?.isPlaying == true ? "Pause" : "Play") {
                        togglePlay()
                    }
                    .keyboardShortcut(.space, modifiers: [])
                    .frame(width: 80)

                    Slider(value: $bpm, in: 40...200, step: 1)
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
        if sequencer?.isPlaying == true {
            sequencer?.stop()
            currentTop = nil
            currentBottom = nil
        } else {
            let top = Line(count: topCount, sound: .edge)
            let bottom = Line(count: bottomCount, sound: .bass)
            let seq = DemoSequencer(audio: audio)
            seq.onStep = { line, index in
                strikeIdCounter += 1
                if line == .top {
                    currentTop = index
                    topStrike = StrikeEvent(sound: .edge, id: strikeIdCounter)
                } else {
                    currentBottom = index
                    bottomStrike = StrikeEvent(sound: .bass, id: strikeIdCounter)
                }
            }
            seq.onBell = {
                bellFlash = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.12) { bellFlash = false }
            }
            seq.start(top: top, bottom: bottom, bpm: bpm)
            sequencer = seq
        }
    }
}

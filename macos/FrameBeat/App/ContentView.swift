import SwiftUI
import FrameBeatCore

// Phase 5 UI: DESIGN.md's "Midnight Workshop" stage/panel split, ported from
// App.vue + Sequencer.vue/TransportControls.vue. Uses the real Phase 3/4
// engine end to end: LiveAudioEngine for sample-accurate playback and
// LiveSequencer for drift-free scheduling, plus Phase 2's AppState for the
// shared model.

struct ContentView: View {
    @State private var appState = AppState()
    @State private var currentTop: Int?
    @State private var currentBottom: Int?
    @State private var progress: Double = 0
    @State private var playing = false

    private let audio = RealtimeAudio()
    @State private var sequencer: LiveSequencer?

    var body: some View {
        VStack(spacing: 24) {
            ZStack {
                RadialGradient(
                    colors: [Theme.Color.panelBorder.opacity(0.55), .clear],
                    center: .center, startRadius: 0, endRadius: 260
                )
                .frame(height: 420)
                .allowsHitTesting(false)

                VStack(spacing: 16) {
                    VStack(spacing: 4) {
                        Text("FrameBeat")
                            .font(Theme.Typography.display())
                        Text("Click the drum, or focus it and drum along on Q W E / I O P")
                            .font(Theme.Typography.body)
                            .foregroundStyle(Theme.Color.labelMuted)
                    }

                    HStack(alignment: .bottom, spacing: 24) {
                        DrumView(
                            onStrike: { sound, _ in audio.play(sound) },
                            playing: playing,
                            strikes: appState.strikes
                        )
                        .frame(width: 340)

                        BellView(trigger: appState.bellTrigger)
                    }
                }
            }

            VStack(spacing: 28) {
                TransportControls(
                    playing: playing,
                    bpm: $appState.bpm,
                    onTogglePlay: togglePlay,
                    onBpmChange: reanchorIfPlaying
                )
                SequencerPanel(
                    top: $appState.top,
                    bottom: $appState.bottom,
                    currentTop: currentTop,
                    currentBottom: currentBottom,
                    progress: progress,
                    onLineChange: reanchorIfPlaying
                )
            }
            .padding(24)
            .background(Theme.Color.panel)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.Color.panelBorder, lineWidth: 1))
        }
        .padding(32)
        .frame(minWidth: 720, minHeight: 820)
        .background(Theme.Color.stage)
        .foregroundStyle(Theme.Color.ink)
        .preferredColorScheme(.dark)
        .onReceive(NotificationCenter.default.publisher(for: .fbTogglePlay)) { _ in togglePlay() }
        .onReceive(NotificationCenter.default.publisher(for: .fbTempoUp)) { _ in
            appState.bpm = min(200, appState.bpm + 5)
            reanchorIfPlaying()
        }
        .onReceive(NotificationCenter.default.publisher(for: .fbTempoDown)) { _ in
            appState.bpm = max(40, appState.bpm - 5)
            reanchorIfPlaying()
        }
        .onReceive(NotificationCenter.default.publisher(for: .fbToggleMute)) { _ in
            appState.bottom.muted.toggle()
            reanchorIfPlaying()
        }
        .onReceive(NotificationCenter.default.publisher(for: .fbResetPattern)) { _ in resetPattern() }
    }

    private func reanchorIfPlaying() {
        if playing {
            sequencer?.update(top: appState.top, bottom: appState.bottom, bpm: appState.bpm)
        }
    }

    private func resetPattern() {
        appState.top = Line(count: 3, sound: .edge)
        appState.bottom = Line(count: 4, sound: .bass)
        reanchorIfPlaying()
    }

    private func togglePlay() {
        if playing {
            sequencer?.stop()
            playing = false
            currentTop = nil
            currentBottom = nil
            progress = 0
        } else {
            let seq = LiveSequencer(engine: audio.engine, top: appState.top, bottom: appState.bottom, bpm: appState.bpm)
            seq.onIndexUpdate = { line, index in
                if line == .top { currentTop = index } else { currentBottom = index }
            }
            seq.onStrike = { line, _, sound in
                appState.recordStrike(sound, on: line)
            }
            seq.onBell = {
                appState.ringBell()
            }
            seq.onProgressUpdate = { value in
                progress = value
            }
            seq.start()
            sequencer = seq
            playing = true
        }
    }
}

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

    /// Measured so the stage (title, drum, bell) can shrink to fit a short
    /// window — a 900pt-tall Mac screen, or a landscape 11" iPad or iPad mini —
    /// while the sequencer panel keeps its full size.
    @State private var containerHeight: CGFloat = 0
    @State private var titleHeight: CGFloat = 0
    @State private var panelHeight: CGFloat = 0

    private static let drumWidth: CGFloat = 340
    private static let drumHeight: CGFloat = drumWidth * 30 / 32
    private static let sectionSpacing: CGFloat = 24
    private static let stageSpacing: CGFloat = 16

    private var outerPadding: CGFloat { containerHeight > 0 && containerHeight < 940 ? 20 : 32 }

    /// 1 when the full-size stage fits; otherwise the drum and bell scale
    /// down together, never below `minStageScale`.
    private var stageScale: CGFloat {
        guard containerHeight > 0, panelHeight > 0 else { return 1 }
        let stage = containerHeight - outerPadding * 2 - Self.sectionSpacing - panelHeight
        let drum = stage - titleHeight - Self.stageSpacing
        return min(1, max(Self.minStageScale, drum / Self.drumHeight))
    }

    private static let minStageScale: CGFloat = 0.45

    var body: some View {
        VStack(spacing: Self.sectionSpacing) {
            ZStack {
                RadialGradient(
                    colors: [Theme.Color.panelBorder.opacity(0.55), .clear],
                    center: .center, startRadius: 0, endRadius: 260 * stageScale
                )
                .frame(height: 420 * stageScale)
                .allowsHitTesting(false)

                VStack(spacing: Self.stageSpacing) {
                    VStack(spacing: 4) {
                        Text("FrameBeat")
                            .font(Theme.Typography.display())
                        #if os(macOS)
                        Text("Click the drum, or focus it and drum along on Q W E / I O P, and ring the bell on B")
                            .font(Theme.Typography.body)
                            .foregroundStyle(Theme.Color.labelMuted)
                        #else
                        Text("Tap the drum or the bell to play")
                            .font(Theme.Typography.body)
                            .foregroundStyle(Theme.Color.labelMuted)
                        #endif
                    }
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { titleHeight = $0 }

                    HStack(alignment: .center, spacing: 24) {
                        DrumView(
                            onStrike: { sound, _ in audio.play(sound) },
                            onRingBell: ringBell,
                            playing: playing,
                            strikes: appState.strikes
                        )
                        .frame(width: Self.drumWidth * stageScale)

                        BellView(trigger: appState.bellTrigger, scale: stageScale, onRing: ringBell)
                            .equatable()
                    }
                }
            }

            VStack(spacing: 28) {
                TransportControls(
                    playing: playing,
                    bpm: $appState.bpm,
                    chimeOnOne: $appState.chimeOnOne,
                    onTogglePlay: togglePlay
                )
                SequencerPanel(
                    top: $appState.top,
                    bottom: $appState.bottom,
                    currentTop: currentTop,
                    currentBottom: currentBottom,
                    progress: progress
                )
            }
            .padding(24)
            .background(Theme.Color.panel)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.Color.panelBorder, lineWidth: 1))
            .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { panelHeight = $0 }
        }
        .padding(outerPadding)
        #if os(macOS)
        .frame(minWidth: 720, minHeight: 750)
        #endif
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { containerHeight = $0 }
        .background(Theme.Color.stage.ignoresSafeArea())
        .foregroundStyle(Theme.Color.ink)
        .preferredColorScheme(.dark)
        .onReceive(NotificationCenter.default.publisher(for: .fbTogglePlay)) { _ in togglePlay() }
        .onReceive(NotificationCenter.default.publisher(for: .fbTempoUp)) { _ in
            appState.bpm = min(200, appState.bpm + 5)
        }
        .onReceive(NotificationCenter.default.publisher(for: .fbTempoDown)) { _ in
            appState.bpm = max(40, appState.bpm - 5)
        }
        .onReceive(NotificationCenter.default.publisher(for: .fbToggleMute)) { _ in
            appState.bottom.muted.toggle()
        }
        .onReceive(NotificationCenter.default.publisher(for: .fbResetPattern)) { _ in resetPattern() }
        // A playing LiveSequencer holds its own copy of top/bottom/bpm (value
        // types, not a live reference), so any change here — from a slider,
        // a sound-picker tap, a step-dot toggle, a mute button, or a menu
        // command — needs to be re-pushed. Observing the state itself here,
        // once, means no individual control needs to remember to call back
        // out after mutating its binding.
        .onChange(of: appState.top) { _, _ in reanchorIfPlaying() }
        .onChange(of: appState.bottom) { _, _ in reanchorIfPlaying() }
        .onChange(of: appState.bpm) { _, _ in reanchorIfPlaying() }
        .onChange(of: appState.chimeOnOne) { _, on in sequencer?.chimeOnOne = on }
    }

    /// Struck by hand (tap, click, or B): rings immediately, whether or not
    /// the sequencer's chime on the one is switched on.
    private func ringBell() {
        audio.playDing()
        appState.ringBell()
    }

    private func reanchorIfPlaying() {
        if playing {
            sequencer?.update(top: appState.top, bottom: appState.bottom, bpm: appState.bpm)
        }
    }

    private func resetPattern() {
        appState.top = Line(count: 3, sound: .edge)
        appState.bottom = Line(count: 4, sound: .bass)
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
            seq.chimeOnOne = appState.chimeOnOne
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

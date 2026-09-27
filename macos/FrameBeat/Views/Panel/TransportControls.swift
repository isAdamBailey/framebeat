import SwiftUI

/// Port of `TransportControls.vue`: the single primary-action pill (Play/
/// Pause, the app's One Glow Rule element) plus the tempo slider/readout.
struct TransportControls: View {
    var playing: Bool
    @Binding var bpm: Double
    @Binding var chimeOnOne: Bool
    var onTogglePlay: () -> Void

    var body: some View {
        HStack(spacing: 20) {
            Button(action: onTogglePlay) {
                HStack(spacing: 10) {
                    Image(systemName: playing ? "pause.fill" : "play.fill")
                        .font(.system(size: 16))
                    Text(playing ? "Pause" : "Play")
                        .font(.system(size: 17, weight: .bold))
                }
                .frame(width: 168, height: 64)
                // Without this, `.plain` sizes the hit target to the
                // label's rendered text/icon bounds rather than the frame
                // above — the pill looks 168x64 but only ~1/8th of it (the
                // "Play"/"Pause" glyphs) actually responds to clicks.
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .background(Theme.Color.bassSky)
            .foregroundStyle(Theme.Color.stage)
            .clipShape(Capsule())
            .shadow(color: Theme.Color.bassSky.opacity(0.35), radius: 14, x: 0, y: 8)

            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .firstTextBaseline) {
                    Text("TEMPO")
                        .font(Theme.Typography.label)
                        .tracking(1.4)
                        .foregroundStyle(Theme.Color.label)
                    Spacer()
                    HStack(alignment: .firstTextBaseline, spacing: 4) {
                        Text("\(Int(bpm))")
                            .font(Theme.Typography.numeral(size: 20))
                        Text("BPM")
                            .font(Theme.Typography.label)
                            .foregroundStyle(Theme.Color.label)
                    }
                }
                Slider(value: $bpm, in: 40...200, step: 1)
                    .tint(Theme.Color.bassSky)
            }

            chimeToggle
        }
    }

    /// The bar-marker chime on "the one", matching `TransportControls.vue`.
    private var chimeToggle: some View {
        TogglePill(pressed: chimeOnOne, action: { chimeOnOne.toggle() }) {
            Image(systemName: chimeOnOne ? "bell.fill" : "bell.slash")
            Text("Chime on 1")
        }
        .help("Ring the bell on the first beat of every bar")
    }
}

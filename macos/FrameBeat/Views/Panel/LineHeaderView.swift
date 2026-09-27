import SwiftUI
import FrameBeatCore

/// Port of `LineHeader.vue`: the mute pill + step-count numeral/slider +
/// labelled sound switch sitting above a sequencer line's dots.
struct LineHeaderView: View {
    let label: String
    @Binding var line: Line

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(label.uppercased())
                    .font(Theme.Typography.label)
                    .tracking(1.4)
                    .foregroundStyle(Theme.Color.label)
                Spacer()
                // Static "Mute" text, so the accessible name contains the
                // visible label; the pressed state carries on/off.
                TogglePill(pressed: line.muted, action: { line.muted.toggle() }) {
                    Image(systemName: line.muted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                    Text("Mute")
                }
                .help("Mute line")
                .accessibilityLabel("Mute \(label)")
            }
            HStack(spacing: 16) {
                Text("\(line.count)")
                    .font(Theme.Typography.numeral(size: 40))
                    .foregroundStyle(Theme.Color.forSound(line.sound))
                    .frame(minWidth: 44, alignment: .center)
                    .monospacedDigit()
                Slider(
                    value: Binding(
                        get: { Double(line.count) },
                        set: { line.count = Int($0) }
                    ),
                    in: 1...16,
                    step: 1
                )
                .tint(Theme.Color.bassSky)
                SoundPicker(sound: $line.sound)
            }
        }
    }
}

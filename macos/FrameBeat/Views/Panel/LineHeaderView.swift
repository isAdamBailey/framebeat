import SwiftUI
import FrameBeatCore

/// Port of `LineHeader.vue`: the mute toggle + step-count numeral/slider +
/// sound picker sitting above a sequencer line's dots.
struct LineHeaderView: View {
    let label: String
    @Binding var line: Line
    var onLineChange: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label.uppercased())
                    .font(Theme.Typography.label)
                    .tracking(1.4)
                    .foregroundStyle(Theme.Color.labelMuted)
                Spacer()
                Button {
                    line.muted.toggle()
                    onLineChange()
                } label: {
                    Image(systemName: line.muted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                        .foregroundStyle(line.muted ? Theme.Color.ink : Theme.Color.labelMuted)
                        .padding(6)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .help(line.muted ? "Unmute line" : "Mute line")
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
                        set: { newValue in
                            line.count = Int(newValue)
                            onLineChange()
                        }
                    ),
                    in: 1...16,
                    step: 1
                )
                .tint(Theme.Color.bassSky)
                SoundPicker(sound: $line.sound, onChange: onLineChange)
            }
        }
    }
}

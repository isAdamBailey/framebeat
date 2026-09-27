import SwiftUI
import FrameBeatCore

/// Port of the sound switch in `LineHeader.vue`: a labelled segmented pill,
/// one segment per `Sound`. The active segment takes its sound's color (the
/// color *is* that sound); the rest stay readable neutral.
struct SoundPicker: View {
    @Binding var sound: Sound

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Sound.allCases, id: \.self) { candidate in
                let active = sound == candidate
                let color = Theme.Color.forSound(candidate)
                Button {
                    sound = candidate
                } label: {
                    HStack(spacing: 6) {
                        Circle().fill(color).frame(width: 8, height: 8)
                        Text(candidate.label)
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .padding(.horizontal, 12)
                    .frame(height: 32)
                    .foregroundStyle(active ? color : Theme.Color.controlText)
                    .background(Capsule().fill(active ? Theme.Color.panelBorder : .clear))
                    .overlay(Capsule().strokeBorder(active ? color : .clear, lineWidth: 1))
                    .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .help("Play \(candidate.label) on this line")
                .accessibilityAddTraits(active ? .isSelected : [])
            }
        }
        .padding(2)
        .background(Capsule().fill(Theme.Color.stage.opacity(0.6)))
        .overlay(Capsule().stroke(Theme.Color.controlBorder.opacity(0.7), lineWidth: 1))
    }
}

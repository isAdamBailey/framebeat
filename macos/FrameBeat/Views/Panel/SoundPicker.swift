import SwiftUI
import FrameBeatCore

/// Port of the sound-picker dots in `LineHeader.vue`: one small solid dot
/// per `Sound`, no text — active dot at full size/opacity, inactive dots
/// smaller and dimmed.
struct SoundPicker: View {
    @Binding var sound: Sound

    var body: some View {
        HStack(spacing: 6) {
            ForEach(Sound.allCases, id: \.self) { candidate in
                Button {
                    sound = candidate
                } label: {
                    Circle()
                        .fill(Theme.Color.forSound(candidate))
                        .frame(width: sound == candidate ? 12 : 8, height: sound == candidate ? 12 : 8)
                        .opacity(sound == candidate ? 1 : 0.35)
                        .frame(width: 22, height: 22)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .help("Play \(candidate.rawValue.capitalized) on this line")
            }
        }
    }
}

import SwiftUI
import FrameBeatCore

/// Port of `Sequencer.vue`: the two `LineHeaderView`/`SequencerLineView`
/// pairs plus the sweeping playhead bar drawn over both lines together.
struct SequencerPanel: View {
    @Binding var top: Line
    @Binding var bottom: Line
    let currentTop: Int?
    let currentBottom: Int?
    let progress: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            LineHeaderView(label: "Top line", line: $top)

            ZStack(alignment: .topLeading) {
                GeometryReader { geo in
                    Rectangle()
                        .fill(Theme.Color.bassSky)
                        .frame(width: 3, height: geo.size.height)
                        .shadow(color: Theme.Color.bassSky.opacity(0.9), radius: 6)
                        .offset(x: geo.size.width * progress - 1.5)
                }
                VStack(spacing: 24) {
                    SequencerLineView(line: $top, current: currentTop)
                    SequencerLineView(line: $bottom, current: currentBottom)
                }
            }
            .frame(height: 88)

            LineHeaderView(label: "Bottom line — sets the pulse", line: $bottom)
        }
    }
}

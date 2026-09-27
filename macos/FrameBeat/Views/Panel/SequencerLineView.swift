import SwiftUI
import FrameBeatCore

/// Port of `SequencerLine.vue`: one row of step dots, each positioned at its
/// true fractional time in the bar (`i / count`) so the playhead sweeps
/// exactly over a dot the moment it fires. On/off dots and the current
/// (playhead) step follow DESIGN.md's Step Dots spec.
struct SequencerLineView: View {
    @Binding var line: Line
    let current: Int?

    var body: some View {
        GeometryReader { geo in
            ZStack {
                Capsule()
                    .fill(Theme.Color.track)
                    .frame(height: 4)
                    .position(x: geo.size.width / 2, y: geo.size.height / 2)
                ForEach(0..<line.count, id: \.self) { i in
                    dotView(i, geo: geo)
                }
            }
        }
        .frame(height: 32)
        .opacity(line.muted ? 0.4 : 1)
        .animation(.easeOut(duration: 0.15), value: line.muted)
    }

    private func dotView(_ i: Int, geo: GeometryProxy) -> some View {
        let on = line.dots[i]
        let isCurrent = current == i
        let x = geo.size.width * (line.count <= 1 ? 0.5 : CGFloat(i) / CGFloat(line.count))
        let size: CGFloat = on ? 24 : 20

        return Button {
            line.dots[i].toggle()
        } label: {
            Circle()
                .fill(on ? Theme.Color.forSound(line.sound) : Theme.Color.panelBorder.opacity(0.5))
                .overlay(Circle().stroke(Theme.Color.controlBorderPressed, lineWidth: on ? 0 : 2))
                .overlay(isCurrent ? Circle().stroke(Color.white.opacity(0.8), lineWidth: 2) : nil)
                .frame(width: size, height: size)
                // Hit area larger than the drawn dot, like the web's before:-inset-2.
                .frame(width: size + 12, height: size + 12)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .scaleEffect(isCurrent ? 1.25 : 1)
        .animation(.easeOut(duration: 0.08), value: isCurrent)
        .position(x: x, y: geo.size.height / 2)
        .help("\(i + 1)")
        .accessibilityLabel("Step \(i + 1)")
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}

import SwiftUI

/// Port of `Bell.vue`: a hanging chime you can strike, that swings, with its clapper
/// swinging independently, plus a soft shine flash — all three replaying
/// whenever `trigger` changes (the bar's "one" being heard, or a hand strike). `Bell.vue` runs
/// three independent Web Animations API `replay()` calls with their own
/// durations/easings; `KeyframeAnimator` is the native SwiftUI equivalent —
/// one `BellPose` with three independently-timed `KeyframeTrack`s, replayed
/// from scratch whenever `trigger` changes.
private struct BellPose {
    var bodyRotation: Double = 0
    var clapperRotation: Double = 0
    var shineOpacity: Double = 0
    var shineOffset: Double = 0
}

struct BellView: View, Equatable {
    let trigger: Int
    /// The art is drawn at its original base size and scaled as a whole,
    /// matching the web bell's `md:[zoom:1.625]`.
    private let artScale: CGFloat = 1.625
    /// Struck by hand: a tap/click (on press, like the drum), or B/Return
    /// while focused. Space stays Play/Pause.
    var onRing: () -> Void

    /// Only `trigger` changes what the bell shows. Comparing on it alone (with
    /// `.equatable()` at the call site) keeps the parent's 120Hz playhead
    /// updates from redrawing the bell just because `onRing` is a closure.
    nonisolated static func == (lhs: BellView, rhs: BellView) -> Bool {
        lhs.trigger == rhs.trigger
    }

    @State private var pressActive = false
    @State private var hovering = false
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(spacing: 6) {
            KeyframeAnimator(initialValue: BellPose(), trigger: trigger) { pose in
                ZStack(alignment: .top) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(Theme.Color.woodNeutralBorder)
                        .frame(width: 8, height: 22)

                    VStack(spacing: 0) {
                        Spacer().frame(height: 22)
                        ZStack(alignment: .top) {
                            bellAssembly(clapperRotation: pose.clapperRotation)
                                .rotationEffect(.degrees(pose.bodyRotation), anchor: .top)

                            Ellipse()
                                .fill(Color.white)
                                .frame(width: 20, height: 40)
                                .blur(radius: 3)
                                .opacity(pose.shineOpacity)
                                .offset(x: pose.shineOffset - 4, y: 24)
                                .allowsHitTesting(false)
                        }
                        // Hover lean hints that the bell can be struck; the
                        // ring swing plays inside it.
                        .rotationEffect(.degrees(hovering ? -4 : 0), anchor: .top)
                        .animation(.easeOut(duration: 0.3), value: hovering)
                    }
                }
                .frame(width: 112, height: 136)
                .scaleEffect(artScale)
                .frame(width: 112 * artScale, height: 136 * artScale)
            } keyframes: { _ in
                KeyframeTrack(\.bodyRotation) {
                    CubicKeyframe(-16, duration: 0.16)
                    CubicKeyframe(12, duration: 0.16)
                    CubicKeyframe(-8, duration: 0.16)
                    CubicKeyframe(4, duration: 0.16)
                    CubicKeyframe(0, duration: 0.16)
                }
                KeyframeTrack(\.clapperRotation) {
                    CubicKeyframe(14, duration: 0.175)
                    CubicKeyframe(-10, duration: 0.175)
                    CubicKeyframe(5, duration: 0.175)
                    CubicKeyframe(0, duration: 0.175)
                }
                KeyframeTrack(\.shineOpacity) {
                    CubicKeyframe(0.6, duration: 0.3)
                    CubicKeyframe(0, duration: 0.3)
                }
                KeyframeTrack(\.shineOffset) {
                    CubicKeyframe(8, duration: 0.3)
                    CubicKeyframe(16, duration: 0.3)
                }
            }
            .contentShape(Rectangle())
            .gesture(
                // Ring on the initial press, not release, matching
                // `Bell.vue`'s @pointerdown and the drum's own gesture.
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        guard !pressActive else { return }
                        pressActive = true
                        onRing()
                    }
                    .onEnded { _ in pressActive = false }
            )
            .onHover { hovering = $0 }
            .focusable()
            .focusEffectDisabled()
            .focused($isFocused)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Theme.Color.bassSky, lineWidth: 2)
                    .padding(-4)
                    .opacity(isFocused ? 1 : 0)
            )
            .onKeyPress(phases: .down) { press in
                // Space is left to the Playback menu's Play/Pause shortcut.
                if press.characters.lowercased() == "b" || press.key == .return {
                    onRing()
                    return .handled
                }
                return .ignored
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Bell")
            .accessibilityHint("Rings the bell")
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { onRing() }

            HStack(spacing: 6) {
                Text("Bell")
                    .font(.system(size: 11))
                    .foregroundStyle(Theme.Color.labelMuted)
                Text("B")
                    .font(.system(size: 10))
                    .foregroundStyle(Theme.Color.woodNeutralStrong.opacity(0.8))
                    .padding(.horizontal, 4)
                    .background(Theme.Color.woodNeutralSurface.opacity(0.6), in: RoundedRectangle(cornerRadius: 4))
                    .overlay(RoundedRectangle(cornerRadius: 4).stroke(Theme.Color.woodNeutralBorder, lineWidth: 1))
            }
            .accessibilityHidden(true)
        }
    }

    private func bellAssembly(clapperRotation: Double) -> some View {
        VStack(spacing: 0) {
            // cord
            Rectangle()
                .fill(Theme.Color.woodNeutral)
                .frame(width: 2, height: 20)
            // hanging loop
            Capsule()
                .strokeBorder(Color(red: 0.56, green: 0.31, blue: 0.09), lineWidth: 2)
                .frame(width: 14, height: 12)
                .offset(y: -2)
            // dome
            UnevenRoundedRectangle(topLeadingRadius: 22, bottomLeadingRadius: 0, bottomTrailingRadius: 0, topTrailingRadius: 22)
                .fill(LinearGradient(
                    colors: [Color(red: 0.97, green: 0.87, blue: 0.52), Color(red: 0.85, green: 0.66, blue: 0.21), Color(red: 0.63, green: 0.43, blue: 0.11)],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ))
                .frame(width: 44, height: 22)
                .offset(y: -4)
            // body tapering to the mouth
            UnevenRoundedRectangle(topLeadingRadius: 0, bottomLeadingRadius: 16, bottomTrailingRadius: 16, topTrailingRadius: 0)
                .fill(LinearGradient(
                    colors: [Color(red: 0.96, green: 0.85, blue: 0.47), Color(red: 0.85, green: 0.66, blue: 0.21), Color(red: 0.56, green: 0.39, blue: 0.1)],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ))
                .frame(width: 56, height: 24)
                .shadow(color: .black.opacity(0.45), radius: 5, x: 0, y: 3)
                .offset(y: -6)
            // flared lip
            Ellipse()
                .fill(LinearGradient(
                    colors: [Color(red: 0.91, green: 0.74, blue: 0.33), Color(red: 0.56, green: 0.39, blue: 0.1)],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ))
                .frame(width: 68, height: 10)
                .shadow(color: .black.opacity(0.5), radius: 3, x: 0, y: 2)
                .offset(y: -10)
            // clapper
            VStack(spacing: 0) {
                Rectangle()
                    .fill(Color(red: 0.36, green: 0.31, blue: 0.27))
                    .frame(width: 2, height: 8)
                Circle()
                    .fill(Color(red: 0.42, green: 0.29, blue: 0.09))
                    .frame(width: 9, height: 9)
            }
            .rotationEffect(.degrees(clapperRotation), anchor: .top)
            .offset(y: -8)
        }
    }
}

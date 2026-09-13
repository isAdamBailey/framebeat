import SwiftUI

/// Port of `Bell.vue`: a small hanging chime that swings, with its clapper
/// swinging independently, plus a soft shine flash — all three replaying
/// whenever `trigger` changes (the bar's "one" being heard). `Bell.vue` runs
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

struct BellView: View {
    let trigger: Int

    var body: some View {
        VStack(spacing: 8) {
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
                        }
                    }
                }
                .frame(width: 128, height: 156)
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

            Text("Bar chime")
                .font(.system(size: 11))
                .foregroundStyle(Theme.Color.labelMuted)
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

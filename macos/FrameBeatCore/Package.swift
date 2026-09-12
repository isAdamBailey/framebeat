// swift-tools-version:6.0
import PackageDescription

let package = Package(
    name: "FrameBeatCore",
    platforms: [.macOS(.v14)], // matches the plan's Phase 1 deployment target
    products: [
        .library(name: "FrameBeatCore", targets: ["FrameBeatCore"]),
        .executable(name: "frame-beat-render", targets: ["frame-beat-render"]),
        .executable(name: "frame-beat-selfcheck", targets: ["frame-beat-selfcheck"]),
    ],
    targets: [
        .target(name: "FrameBeatCore"),
        .executableTarget(name: "frame-beat-render", dependencies: ["FrameBeatCore"]),
        .executableTarget(name: "frame-beat-selfcheck", dependencies: ["FrameBeatCore"]),
        .testTarget(name: "FrameBeatCoreTests", dependencies: ["FrameBeatCore"]),
    ]
)

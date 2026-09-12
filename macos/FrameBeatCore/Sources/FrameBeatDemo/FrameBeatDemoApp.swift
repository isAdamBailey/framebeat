import SwiftUI

@main
struct FrameBeatDemoApp: App {
    var body: some Scene {
        WindowGroup("FrameBeat (Swift demo)") {
            ContentView()
        }
        .windowResizability(.contentSize)
    }
}

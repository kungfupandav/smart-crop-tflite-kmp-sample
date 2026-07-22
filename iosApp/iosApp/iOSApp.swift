import SwiftUI
import shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // The app is always light-themed; forcing the light color scheme
                // makes the status bar use dark (contrasting) content.
                .preferredColorScheme(.light)
                .ignoresSafeArea()
        }
    }
}

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

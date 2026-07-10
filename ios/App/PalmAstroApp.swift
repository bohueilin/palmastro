import SwiftUI

/// PalmAstro iOS entry point (PRD §9, §48). SwiftUI + MVVM; engines come from
/// the PalmAstroKit Swift package and run entirely on-device.
@main
struct PalmAstroApp: App {

    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
        }
    }
}

/// Routes between onboarding and the main experience.
struct RootView: View {

    @EnvironmentObject private var model: AppModel

    var body: some View {
        if model.profile.onboardingComplete {
            MainTabView()
        } else {
            OnboardingView()
        }
    }
}

struct MainTabView: View {

    @EnvironmentObject private var model: AppModel

    var body: some View {
        TabView {
            NavigationStack {
                ResultsView()
            }
            .tabItem {
                Label("tab_results", systemImage: "chart.bar.doc.horizontal")
            }

            NavigationStack {
                JournalView()
            }
            .tabItem {
                Label("tab_journal", systemImage: "book.closed")
            }

            NavigationStack {
                SettingsView()
            }
            .tabItem {
                Label("tab_settings", systemImage: "gearshape")
            }
        }
    }
}

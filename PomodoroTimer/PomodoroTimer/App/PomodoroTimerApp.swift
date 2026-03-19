import SwiftUI
import WidgetKit

@main
struct PomodoroTimerApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            SettingsView()
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                // フォアグラウンド復帰時にウィジェットを最新状態に更新
                WidgetCenter.shared.reloadAllTimelines()
            }
        }
    }
}

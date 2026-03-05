import SwiftUI
import WidgetKit

struct SettingsView: View {
    @AppStorage("workDuration", store: UserDefaults(suiteName: SharedConstants.appGroupID))
    private var workDurationMinutes: Int = 25

    @AppStorage("shortBreakDuration", store: UserDefaults(suiteName: SharedConstants.appGroupID))
    private var shortBreakMinutes: Int = 5

    @AppStorage("longBreakDuration", store: UserDefaults(suiteName: SharedConstants.appGroupID))
    private var longBreakMinutes: Int = 15

    @AppStorage("notificationsEnabled", store: UserDefaults(suiteName: SharedConstants.appGroupID))
    private var notificationsEnabled: Bool = true

    @State private var authStatus: Bool = false
    @State private var showNotificationAlert = false

    var body: some View {
        NavigationStack {
            Form {
                Section("タイマー設定") {
                    Stepper(
                        "作業時間: \(workDurationMinutes) 分",
                        value: $workDurationMinutes,
                        in: 1...60,
                        step: 5,
                        onEditingChanged: { _ in applySettings() }
                    )
                    Stepper(
                        "短い休憩: \(shortBreakMinutes) 分",
                        value: $shortBreakMinutes,
                        in: 1...30,
                        step: 1,
                        onEditingChanged: { _ in applySettings() }
                    )
                    Stepper(
                        "長い休憩: \(longBreakMinutes) 分",
                        value: $longBreakMinutes,
                        in: 5...60,
                        step: 5,
                        onEditingChanged: { _ in applySettings() }
                    )
                }

                Section("通知") {
                    Toggle("フェーズ終了時に通知", isOn: $notificationsEnabled)
                        .onChange(of: notificationsEnabled) { _, newValue in
                            if newValue {
                                Task { await requestNotificationPermission() }
                            }
                            applySettings()
                        }

                    if !authStatus && notificationsEnabled {
                        Button("通知の許可をシステム設定で確認") {
                            if let url = URL(string: UIApplication.openSettingsURLString) {
                                UIApplication.shared.open(url)
                            }
                        }
                        .foregroundStyle(.orange)
                        .font(.caption)
                    }
                }

                Section("ポモドーロについて") {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("ポモドーロテクニックとは")
                            .fontWeight(.semibold)
                        Text("作業を25分のブロックに分け、5分の休憩を取るサイクルを繰り返す時間管理術です。4サイクル後に長い休憩（15分）を取ります。")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("設定")
            .task {
                authStatus = await NotificationService.shared.requestAuthorization()
            }
        }
    }

    private func applySettings() {
        var state = SharedDataStore.shared.loadState()
        // タイマーが停止中のみ設定を即時反映
        if !state.isRunning {
            state.workDuration = workDurationMinutes * 60
            state.shortBreakDuration = shortBreakMinutes * 60
            state.longBreakDuration = longBreakMinutes * 60
            if state.phase == .idle || state.phase == .work {
                state.remainingSeconds = state.workDuration
            }
        }
        state.notificationsEnabled = notificationsEnabled
        SharedDataStore.shared.saveAndReloadWidgets(state)
    }

    private func requestNotificationPermission() async {
        authStatus = await NotificationService.shared.requestAuthorization()
    }
}

#Preview {
    SettingsView()
}

import AppIntents
import WidgetKit

// MARK: - Start

struct StartTimerIntent: AppIntent {
    static var title: LocalizedStringResource = "タイマー開始"
    static var description = IntentDescription("ポモドーロタイマーを開始します")

    func perform() async throws -> some IntentResult {
        var state = SharedDataStore.shared.loadState()
        guard !state.isRunning else { return .result() }

        // idle の場合はまず work フェーズへ移行
        if state.phase == .idle {
            state.phase = .work
            state.remainingSeconds = state.workDuration
        }

        let duration = state.remainingSeconds > 0 ? state.remainingSeconds : state.currentPhaseDuration
        state.endDate = Date().addingTimeInterval(Double(duration))
        state.isRunning = true

        SharedDataStore.shared.save(state)

        if state.notificationsEnabled, let endDate = state.endDate {
            await NotificationService.shared.schedule(endDate: endDate, phase: state.phase)
        }

        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

// MARK: - Pause

struct PauseTimerIntent: AppIntent {
    static var title: LocalizedStringResource = "タイマー一時停止"
    static var description = IntentDescription("ポモドーロタイマーを一時停止します")

    func perform() async throws -> some IntentResult {
        var state = SharedDataStore.shared.loadState()
        guard state.isRunning, let endDate = state.endDate else { return .result() }

        let remaining = max(0, Int(endDate.timeIntervalSinceNow))
        state.remainingSeconds = remaining
        state.isRunning = false
        state.endDate = nil

        SharedDataStore.shared.save(state)
        NotificationService.shared.cancelPending()
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

// MARK: - Reset

struct ResetTimerIntent: AppIntent {
    static var title: LocalizedStringResource = "タイマーリセット"
    static var description = IntentDescription("ポモドーロタイマーをリセットします")

    func perform() async throws -> some IntentResult {
        var state = SharedDataStore.shared.loadState()
        state.isRunning = false
        state.endDate = nil
        state.phase = .idle
        state.remainingSeconds = state.workDuration
        state.sessionNumber = 1
        state.completedPomodoros = 0

        SharedDataStore.shared.save(state)
        NotificationService.shared.cancelPending()
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

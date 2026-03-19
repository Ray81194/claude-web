import Foundation

struct TimerStateDTO: Codable, Equatable {

    enum Phase: String, Codable, CaseIterable {
        case idle
        case work
        case shortBreak
        case longBreak

        var label: String {
            switch self {
            case .idle:       return "待機中"
            case .work:       return "作業中"
            case .shortBreak: return "短い休憩"
            case .longBreak:  return "長い休憩"
            }
        }
    }

    // MARK: - Timer State
    var phase: Phase
    var isRunning: Bool
    /// タイマー終了予定時刻（バックグラウンド移行後もずれない）
    var endDate: Date?
    /// 一時停止中の残り秒数
    var remainingSeconds: Int

    // MARK: - Session
    var completedPomodoros: Int
    /// 現在の連続セッション番号（1〜4、4回でlongBreak）
    var sessionNumber: Int

    // MARK: - User Settings
    var workDuration: Int       // 秒（デフォルト1500 = 25分）
    var shortBreakDuration: Int // 秒（デフォルト300 = 5分）
    var longBreakDuration: Int  // 秒（デフォルト900 = 15分）
    var notificationsEnabled: Bool

    // MARK: - Computed

    var currentPhaseDuration: Int {
        switch phase {
        case .idle:       return workDuration
        case .work:       return workDuration
        case .shortBreak: return shortBreakDuration
        case .longBreak:  return longBreakDuration
        }
    }

    /// 次のフェーズに遷移した状態を返す
    var nextPhaseState: TimerStateDTO {
        var next = self
        next.isRunning = false
        next.endDate = nil

        switch phase {
        case .idle:
            next.phase = .work
        case .work:
            if sessionNumber >= 4 {
                next.phase = .longBreak
            } else {
                next.phase = .shortBreak
            }
            next.completedPomodoros += 1
        case .shortBreak:
            next.phase = .work
            next.sessionNumber = min(sessionNumber + 1, 4)
        case .longBreak:
            next.phase = .idle
            next.sessionNumber = 1
            next.completedPomodoros = 0
        }

        next.remainingSeconds = next.currentPhaseDuration
        return next
    }

    // MARK: - Default

    static var defaultState: TimerStateDTO {
        TimerStateDTO(
            phase: .idle,
            isRunning: false,
            endDate: nil,
            remainingSeconds: 1500,
            completedPomodoros: 0,
            sessionNumber: 1,
            workDuration: 1500,
            shortBreakDuration: 300,
            longBreakDuration: 900,
            notificationsEnabled: true
        )
    }
}

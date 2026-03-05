import Foundation
import UserNotifications

final class NotificationService {

    static let shared = NotificationService()
    private static let requestID = "pomodoro.timer"

    private init() {}

    // MARK: - Authorization

    @discardableResult
    func requestAuthorization() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let options: UNAuthorizationOptions = [.alert, .sound, .badge]
        do {
            return try await center.requestAuthorization(options: options)
        } catch {
            return false
        }
    }

    func authorizationStatus() async -> UNAuthorizationStatus {
        await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
    }

    // MARK: - Schedule

    func schedule(endDate: Date, phase: TimerStateDTO.Phase) async {
        let status = await authorizationStatus()
        guard status == .authorized else { return }

        cancelPending()

        let content = UNMutableNotificationContent()
        switch phase {
        case .work:
            content.title = "作業時間終了"
            content.body = "お疲れ様でした。休憩を始めましょう。"
        case .shortBreak:
            content.title = "短い休憩終了"
            content.body = "さあ、次のポモドーロを始めましょう！"
        case .longBreak:
            content.title = "長い休憩終了"
            content.body = "リフレッシュできましたか？作業を再開しましょう。"
        case .idle:
            return
        }
        content.sound = .default
        content.interruptionLevel = .timeSensitive

        let triggerDate = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: endDate
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: triggerDate, repeats: false)
        let request = UNNotificationRequest(
            identifier: Self.requestID,
            content: content,
            trigger: trigger
        )

        try? await UNUserNotificationCenter.current().add(request)
    }

    // MARK: - Cancel

    func cancelPending() {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [Self.requestID])
    }
}

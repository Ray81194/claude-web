import WidgetKit
import SwiftUI

@main
struct PomodoroTimerWidget: Widget {
    let kind: String = "PomodoroTimerWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PomodoroTimelineProvider()) { entry in
            WidgetView(entry: entry)
        }
        .configurationDisplayName("ポモドーロタイマー")
        .description("ポモドーロタイマーをウィジェットで操作・確認できます")
        .supportedFamilies([
            .systemSmall,
            .systemMedium,
            .accessoryCircular,
            .accessoryRectangular
        ])
        .contentMarginsDisabled()
    }
}

// MARK: - Root Widget View

struct WidgetView: View {
    @Environment(\.widgetFamily) var family
    let entry: WidgetTimerEntry

    var body: some View {
        switch family {
        case .systemSmall:
            WidgetSmallView(entry: entry)
        case .systemMedium:
            WidgetMediumView(entry: entry)
        case .accessoryCircular:
            WidgetCircularView(entry: entry)
        case .accessoryRectangular:
            WidgetRectangularView(entry: entry)
        default:
            WidgetSmallView(entry: entry)
        }
    }
}

// MARK: - Previews

#Preview(as: .systemSmall) {
    PomodoroTimerWidget()
} timeline: {
    WidgetTimerEntry(
        date: .now,
        dto: .defaultState,
        displayMode: .idle
    )
    WidgetTimerEntry(
        date: .now,
        dto: TimerStateDTO(
            phase: .work,
            isRunning: true,
            endDate: Date().addingTimeInterval(24 * 60 + 37),
            remainingSeconds: 24 * 60 + 37,
            completedPomodoros: 1,
            sessionNumber: 2,
            workDuration: 1500,
            shortBreakDuration: 300,
            longBreakDuration: 900,
            notificationsEnabled: true
        ),
        displayMode: .countdown(endDate: Date().addingTimeInterval(24 * 60 + 37))
    )
}

#Preview(as: .systemMedium) {
    PomodoroTimerWidget()
} timeline: {
    WidgetTimerEntry(
        date: .now,
        dto: TimerStateDTO(
            phase: .work,
            isRunning: false,
            endDate: nil,
            remainingSeconds: 1500,
            completedPomodoros: 0,
            sessionNumber: 1,
            workDuration: 1500,
            shortBreakDuration: 300,
            longBreakDuration: 900,
            notificationsEnabled: true
        ),
        displayMode: .idle
    )
}

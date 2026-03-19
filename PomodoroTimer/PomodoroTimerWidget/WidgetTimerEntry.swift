import WidgetKit
import Foundation

struct WidgetTimerEntry: TimelineEntry {
    let date: Date
    let dto: TimerStateDTO

    enum DisplayMode {
        case idle
        case countdown(endDate: Date)
        case finished
    }

    let displayMode: DisplayMode
}

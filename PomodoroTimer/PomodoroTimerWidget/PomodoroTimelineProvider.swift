import WidgetKit
import Foundation

struct PomodoroTimelineProvider: TimelineProvider {

    typealias Entry = WidgetTimerEntry

    func placeholder(in context: Context) -> WidgetTimerEntry {
        WidgetTimerEntry(
            date: .now,
            dto: .defaultState,
            displayMode: .countdown(endDate: Date().addingTimeInterval(25 * 60))
        )
    }

    func getSnapshot(
        in context: Context,
        completion: @escaping (WidgetTimerEntry) -> Void
    ) {
        let dto = SharedDataStore.shared.loadState()
        let entry = makeCurrentEntry(dto: dto)
        completion(entry)
    }

    func getTimeline(
        in context: Context,
        completion: @escaping (Timeline<WidgetTimerEntry>) -> Void
    ) {
        let dto = SharedDataStore.shared.loadState()

        if dto.isRunning, let endDate = dto.endDate {
            // エントリー1: 現在 — カウントダウン中
            let currentEntry = WidgetTimerEntry(
                date: .now,
                dto: dto,
                displayMode: .countdown(endDate: endDate)
            )

            // エントリー2: 終了時刻 — 完了表示
            var finishedDTO = dto
            finishedDTO.isRunning = false
            finishedDTO.endDate = nil
            let finishedEntry = WidgetTimerEntry(
                date: endDate,
                dto: finishedDTO,
                displayMode: .finished
            )

            // .atEnd でシステムが終了後に getTimeline を再呼び出しする
            completion(Timeline(entries: [currentEntry, finishedEntry], policy: .atEnd))
        } else {
            // 停止中: 静的表示、30分後に再確認
            let entry = makeCurrentEntry(dto: dto)
            let nextUpdate = Date().addingTimeInterval(30 * 60)
            completion(Timeline(entries: [entry], policy: .after(nextUpdate)))
        }
    }

    // MARK: - Private

    private func makeCurrentEntry(dto: TimerStateDTO) -> WidgetTimerEntry {
        let displayMode: WidgetTimerEntry.DisplayMode
        if dto.isRunning, let endDate = dto.endDate {
            displayMode = .countdown(endDate: endDate)
        } else if dto.phase == .idle {
            displayMode = .idle
        } else {
            displayMode = .finished
        }
        return WidgetTimerEntry(date: .now, dto: dto, displayMode: displayMode)
    }
}

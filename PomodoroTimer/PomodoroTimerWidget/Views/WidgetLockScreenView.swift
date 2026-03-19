import SwiftUI
import WidgetKit

// MARK: - accessoryCircular

struct WidgetCircularView: View {
    let entry: WidgetTimerEntry

    var body: some View {
        switch entry.displayMode {
        case .countdown(let endDate):
            ProgressView(timerInterval: Date()...endDate, countsDown: true) {
                Image(systemName: "timer")
            } currentValueLabel: {
                Text(endDate, style: .timer)
                    .font(.system(size: 10, design: .monospaced))
                    .minimumScaleFactor(0.5)
            }
            .progressViewStyle(.circular)

        case .idle, .finished:
            VStack(spacing: 1) {
                Image(systemName: entry.dto.isRunning ? "timer" : "timer.circle")
                    .font(.system(size: 14))
                Text(entry.dto.phase.label)
                    .font(.system(size: 8))
                    .minimumScaleFactor(0.5)
            }
        }
    }
}

// MARK: - accessoryRectangular

struct WidgetRectangularView: View {
    let entry: WidgetTimerEntry

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 1) {
                Text(entry.dto.phase.label)
                    .font(.caption2)
                    .fontWeight(.semibold)

                switch entry.displayMode {
                case .countdown(let endDate):
                    Text(endDate, style: .timer)
                        .font(.system(size: 20, weight: .bold, design: .monospaced))
                        .minimumScaleFactor(0.5)
                case .idle:
                    Text("準備完了")
                        .font(.system(size: 16, weight: .semibold))
                case .finished:
                    Text("完了 ✓")
                        .font(.system(size: 16, weight: .semibold))
                }
            }
            Spacer()
        }
    }
}

import SwiftUI
import WidgetKit

struct WidgetSmallView: View {
    let entry: WidgetTimerEntry

    var body: some View {
        VStack(spacing: 4) {
            // フェーズ名
            Text(entry.dto.phase.label)
                .font(.caption2)
                .fontWeight(.semibold)
                .foregroundStyle(.secondary)

            // カウントダウン or 停止表示
            switch entry.displayMode {
            case .countdown(let endDate):
                Text(endDate, style: .timer)
                    .font(.system(size: 34, weight: .bold, design: .monospaced))
                    .minimumScaleFactor(0.5)

            case .idle:
                Text(formattedDuration(entry.dto.workDuration))
                    .font(.system(size: 34, weight: .bold, design: .monospaced))
                    .foregroundStyle(.secondary)

            case .finished:
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 34))
                    .foregroundStyle(.green)
            }

            // セッション番号（ポモドーロ絵文字）
            HStack(spacing: 2) {
                ForEach(1...4, id: \.self) { i in
                    Circle()
                        .fill(i <= entry.dto.sessionNumber ? Color.accentColor : Color.secondary.opacity(0.3))
                        .frame(width: 6, height: 6)
                }
            }
        }
        .containerBackground(.fill.tertiary, for: .widget)
    }

    private func formattedDuration(_ seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%02d:%02d", m, s)
    }
}

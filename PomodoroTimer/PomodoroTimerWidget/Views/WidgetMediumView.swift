import SwiftUI
import WidgetKit

/// メインのウィジェット（systemMedium）
/// カウントダウン表示 + 再生/停止/リセットボタンを含む
struct WidgetMediumView: View {
    let entry: WidgetTimerEntry

    var body: some View {
        HStack(spacing: 16) {
            // 左: タイマー表示
            VStack(alignment: .leading, spacing: 6) {
                Text(entry.dto.phase.label)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(.secondary)

                switch entry.displayMode {
                case .countdown(let endDate):
                    Text(endDate, style: .timer)
                        .font(.system(size: 44, weight: .bold, design: .monospaced))
                        .minimumScaleFactor(0.5)

                case .idle:
                    Text(formattedDuration(entry.dto.workDuration))
                        .font(.system(size: 44, weight: .bold, design: .monospaced))
                        .foregroundStyle(.secondary)

                case .finished:
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 36))
                            .foregroundStyle(.green)
                        Text("完了！")
                            .font(.title2)
                            .fontWeight(.bold)
                    }
                }

                // セッション進捗ドット
                HStack(spacing: 4) {
                    Text("セッション")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    ForEach(1...4, id: \.self) { i in
                        Circle()
                            .fill(i <= entry.dto.sessionNumber ? Color.accentColor : Color.secondary.opacity(0.3))
                            .frame(width: 7, height: 7)
                    }
                }
            }

            Spacer()

            // 右: コントロールボタン
            VStack(spacing: 12) {
                // 再生 / 一時停止
                if entry.dto.isRunning {
                    Button(intent: PauseTimerIntent()) {
                        Image(systemName: "pause.circle.fill")
                            .font(.system(size: 44))
                            .foregroundStyle(.primary)
                    }
                    .buttonStyle(.plain)
                } else {
                    Button(intent: StartTimerIntent()) {
                        Image(systemName: "play.circle.fill")
                            .font(.system(size: 44))
                            .foregroundStyle(.accentColor)
                    }
                    .buttonStyle(.plain)
                }

                // リセット
                Button(intent: ResetTimerIntent()) {
                    Image(systemName: "arrow.counterclockwise.circle")
                        .font(.system(size: 28))
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding()
        .containerBackground(.fill.tertiary, for: .widget)
    }

    private func formattedDuration(_ seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%02d:%02d", m, s)
    }
}

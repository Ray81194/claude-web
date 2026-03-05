import Foundation
import WidgetKit

/// アプリとウィジェット拡張の両方から使用できる共有データストア。
/// App Groups の UserDefaults を使ってデータを永続化する。
final class SharedDataStore {

    static let shared = SharedDataStore()

    private let defaults: UserDefaults

    private init() {
        guard let defaults = UserDefaults(suiteName: SharedConstants.appGroupID) else {
            fatalError("App Group '\(SharedConstants.appGroupID)' が設定されていません。Xcodeの Signing & Capabilities で App Groups を追加してください。")
        }
        self.defaults = defaults
    }

    // MARK: - Read

    func loadState() -> TimerStateDTO {
        guard
            let data = defaults.data(forKey: SharedConstants.timerStateKey),
            let dto = try? JSONDecoder().decode(TimerStateDTO.self, from: data)
        else {
            return .defaultState
        }
        return dto
    }

    // MARK: - Write

    func save(_ dto: TimerStateDTO) {
        guard let data = try? JSONEncoder().encode(dto) else { return }
        defaults.set(data, forKey: SharedConstants.timerStateKey)
    }

    /// 保存してウィジェットのタイムラインを再読み込みする（アプリ側から呼ぶ）
    func saveAndReloadWidgets(_ dto: TimerStateDTO) {
        save(dto)
        WidgetCenter.shared.reloadAllTimelines()
    }
}

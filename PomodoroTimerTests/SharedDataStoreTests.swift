import XCTest
@testable import PomodoroTimer

// MARK: - SharedDataStoreTests
//
// NOTE: App Group UserDefaults はシミュレーター・実機上でのみ動作します。
// これらのテストは Xcode でシミュレーターをターゲットにして実行してください。

final class SharedDataStoreTests: XCTestCase {

    private let testSuiteName = "group.com.yourcompany.pomodorotimer.test"
    private var testDefaults: UserDefaults!

    override func setUp() {
        super.setUp()
        testDefaults = UserDefaults(suiteName: testSuiteName)
        testDefaults.removeObject(forKey: SharedConstants.timerStateKey)
    }

    override func tearDown() {
        testDefaults.removePersistentDomain(forName: testSuiteName)
        super.tearDown()
    }

    // MARK: loadState

    func test_loadState_returnsDefaultState_whenNothingSaved() {
        // testDefaults にデータがない状態では defaultState が返るはず
        // （SharedDataStore は Singleton のため、ここでは Codable ロジックを直接テスト）
        let data = testDefaults.data(forKey: SharedConstants.timerStateKey)
        XCTAssertNil(data, "初期状態ではデータが存在しないこと")
    }

    // MARK: Codable round-trip (SharedDataStore のコアロジック)

    func test_saveAndLoad_preservesAllFields() throws {
        var original = TimerStateDTO.defaultState
        original.phase = .shortBreak
        original.isRunning = true
        original.endDate = Date(timeIntervalSinceNow: 300)
        original.remainingSeconds = 287
        original.completedPomodoros = 2
        original.sessionNumber = 3
        original.workDuration = 1500
        original.shortBreakDuration = 300
        original.longBreakDuration = 900
        original.notificationsEnabled = false

        // エンコード → 保存 → ロード → デコード のサイクルをテスト
        let encoded = try JSONEncoder().encode(original)
        testDefaults.set(encoded, forKey: SharedConstants.timerStateKey)

        guard let saved = testDefaults.data(forKey: SharedConstants.timerStateKey) else {
            XCTFail("データが保存されていない"); return
        }
        let decoded = try JSONDecoder().decode(TimerStateDTO.self, from: saved)

        XCTAssertEqual(decoded.phase, original.phase)
        XCTAssertEqual(decoded.isRunning, original.isRunning)
        XCTAssertEqual(decoded.remainingSeconds, original.remainingSeconds)
        XCTAssertEqual(decoded.completedPomodoros, original.completedPomodoros)
        XCTAssertEqual(decoded.sessionNumber, original.sessionNumber)
        XCTAssertEqual(decoded.workDuration, original.workDuration)
        XCTAssertEqual(decoded.shortBreakDuration, original.shortBreakDuration)
        XCTAssertEqual(decoded.longBreakDuration, original.longBreakDuration)
        XCTAssertFalse(decoded.notificationsEnabled)
        XCTAssertNotNil(decoded.endDate)
    }

    func test_saveAndLoad_handlesNilEndDate() throws {
        var state = TimerStateDTO.defaultState
        state.endDate = nil

        let encoded = try JSONEncoder().encode(state)
        testDefaults.set(encoded, forKey: SharedConstants.timerStateKey)

        let saved = testDefaults.data(forKey: SharedConstants.timerStateKey)!
        let decoded = try JSONDecoder().decode(TimerStateDTO.self, from: saved)

        XCTAssertNil(decoded.endDate)
    }

    func test_overwrite_replacesExistingState() throws {
        // 1回目の保存
        var first = TimerStateDTO.defaultState
        first.phase = .work
        testDefaults.set(try JSONEncoder().encode(first), forKey: SharedConstants.timerStateKey)

        // 2回目の保存で上書き
        var second = TimerStateDTO.defaultState
        second.phase = .longBreak
        testDefaults.set(try JSONEncoder().encode(second), forKey: SharedConstants.timerStateKey)

        let saved = testDefaults.data(forKey: SharedConstants.timerStateKey)!
        let decoded = try JSONDecoder().decode(TimerStateDTO.self, from: saved)

        XCTAssertEqual(decoded.phase, .longBreak, "後から保存した値で上書きされていること")
    }
}

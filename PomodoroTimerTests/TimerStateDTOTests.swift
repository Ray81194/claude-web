import XCTest
@testable import PomodoroTimer

// MARK: - TimerStateDTOTests

final class TimerStateDTOTests: XCTestCase {

    // MARK: defaultState

    func test_defaultState_isIdle() {
        let state = TimerStateDTO.defaultState
        XCTAssertEqual(state.phase, .idle)
        XCTAssertFalse(state.isRunning)
        XCTAssertNil(state.endDate)
        XCTAssertEqual(state.sessionNumber, 1)
        XCTAssertEqual(state.completedPomodoros, 0)
    }

    func test_defaultState_durationsAreCorrect() {
        let state = TimerStateDTO.defaultState
        XCTAssertEqual(state.workDuration, 1500)       // 25分
        XCTAssertEqual(state.shortBreakDuration, 300)  // 5分
        XCTAssertEqual(state.longBreakDuration, 900)   // 15分
    }

    // MARK: currentPhaseDuration

    func test_currentPhaseDuration_idle_returnsWorkDuration() {
        var state = TimerStateDTO.defaultState
        state.phase = .idle
        XCTAssertEqual(state.currentPhaseDuration, state.workDuration)
    }

    func test_currentPhaseDuration_work_returnsWorkDuration() {
        var state = TimerStateDTO.defaultState
        state.phase = .work
        XCTAssertEqual(state.currentPhaseDuration, state.workDuration)
    }

    func test_currentPhaseDuration_shortBreak_returnsShortBreakDuration() {
        var state = TimerStateDTO.defaultState
        state.phase = .shortBreak
        XCTAssertEqual(state.currentPhaseDuration, state.shortBreakDuration)
    }

    func test_currentPhaseDuration_longBreak_returnsLongBreakDuration() {
        var state = TimerStateDTO.defaultState
        state.phase = .longBreak
        XCTAssertEqual(state.currentPhaseDuration, state.longBreakDuration)
    }

    // MARK: nextPhaseState — work → shortBreak (sessions 1〜3)

    func test_nextPhase_workSession1_goesToShortBreak() {
        var state = TimerStateDTO.defaultState
        state.phase = .work
        state.sessionNumber = 1

        let next = state.nextPhaseState
        XCTAssertEqual(next.phase, .shortBreak)
        XCTAssertEqual(next.completedPomodoros, 1)
        XCTAssertFalse(next.isRunning)
        XCTAssertNil(next.endDate)
    }

    func test_nextPhase_workSession3_goesToShortBreak() {
        var state = TimerStateDTO.defaultState
        state.phase = .work
        state.sessionNumber = 3

        let next = state.nextPhaseState
        XCTAssertEqual(next.phase, .shortBreak)
    }

    // MARK: nextPhaseState — work session4 → longBreak

    func test_nextPhase_workSession4_goesToLongBreak() {
        var state = TimerStateDTO.defaultState
        state.phase = .work
        state.sessionNumber = 4

        let next = state.nextPhaseState
        XCTAssertEqual(next.phase, .longBreak)
        XCTAssertEqual(next.completedPomodoros, 1)
    }

    // MARK: nextPhaseState — shortBreak → work

    func test_nextPhase_shortBreak_goesToWork() {
        var state = TimerStateDTO.defaultState
        state.phase = .shortBreak
        state.sessionNumber = 1

        let next = state.nextPhaseState
        XCTAssertEqual(next.phase, .work)
        XCTAssertEqual(next.sessionNumber, 2)
    }

    func test_nextPhase_shortBreak_sessionNumberDoesNotExceed4() {
        var state = TimerStateDTO.defaultState
        state.phase = .shortBreak
        state.sessionNumber = 4

        let next = state.nextPhaseState
        XCTAssertEqual(next.sessionNumber, 4) // min(4+1, 4) = 4
    }

    // MARK: nextPhaseState — longBreak → idle (reset)

    func test_nextPhase_longBreak_resetsToIdle() {
        var state = TimerStateDTO.defaultState
        state.phase = .longBreak
        state.sessionNumber = 4
        state.completedPomodoros = 4

        let next = state.nextPhaseState
        XCTAssertEqual(next.phase, .idle)
        XCTAssertEqual(next.sessionNumber, 1)
        XCTAssertEqual(next.completedPomodoros, 0)
    }

    // MARK: nextPhaseState — remainingSeconds updated

    func test_nextPhase_remainingSecondsUpdatedToNextPhaseDuration() {
        var state = TimerStateDTO.defaultState
        state.phase = .work
        state.sessionNumber = 1

        let next = state.nextPhaseState
        // work → shortBreak なので remainingSeconds = shortBreakDuration
        XCTAssertEqual(next.remainingSeconds, state.shortBreakDuration)
    }

    // MARK: Phase.label

    func test_phaseLabels() {
        XCTAssertEqual(TimerStateDTO.Phase.idle.label, "待機中")
        XCTAssertEqual(TimerStateDTO.Phase.work.label, "作業中")
        XCTAssertEqual(TimerStateDTO.Phase.shortBreak.label, "短い休憩")
        XCTAssertEqual(TimerStateDTO.Phase.longBreak.label, "長い休憩")
    }

    // MARK: Full Pomodoro Cycle

    func test_fullPomodoroCycle_4WorkSessionsLeadToLongBreak() {
        var state = TimerStateDTO.defaultState
        state.phase = .work
        state.sessionNumber = 1

        // 1回目: work → shortBreak
        state = state.nextPhaseState
        XCTAssertEqual(state.phase, .shortBreak)

        // shortBreak → work
        state = state.nextPhaseState
        XCTAssertEqual(state.phase, .work)
        XCTAssertEqual(state.sessionNumber, 2)

        // 2回目: work → shortBreak
        state = state.nextPhaseState
        XCTAssertEqual(state.phase, .shortBreak)

        // shortBreak → work
        state = state.nextPhaseState
        XCTAssertEqual(state.phase, .work)
        XCTAssertEqual(state.sessionNumber, 3)

        // 3回目: work → shortBreak
        state = state.nextPhaseState
        XCTAssertEqual(state.phase, .shortBreak)

        // shortBreak → work
        state = state.nextPhaseState
        XCTAssertEqual(state.phase, .work)
        XCTAssertEqual(state.sessionNumber, 4)

        // 4回目: work → longBreak（4セッション目はlongBreak）
        state = state.nextPhaseState
        XCTAssertEqual(state.phase, .longBreak)
        XCTAssertEqual(state.completedPomodoros, 4)

        // longBreak → idle（リセット）
        state = state.nextPhaseState
        XCTAssertEqual(state.phase, .idle)
        XCTAssertEqual(state.sessionNumber, 1)
        XCTAssertEqual(state.completedPomodoros, 0)
    }

    // MARK: Codable

    func test_timerStateDTO_isEncodableAndDecodable() throws {
        var original = TimerStateDTO.defaultState
        original.phase = .work
        original.isRunning = true
        original.endDate = Date(timeIntervalSinceReferenceDate: 12345)
        original.remainingSeconds = 900
        original.sessionNumber = 2
        original.completedPomodoros = 3

        let data = try JSONEncoder().encode(original)
        let decoded = try JSONDecoder().decode(TimerStateDTO.self, from: data)

        XCTAssertEqual(decoded.phase, original.phase)
        XCTAssertEqual(decoded.isRunning, original.isRunning)
        XCTAssertEqual(decoded.endDate?.timeIntervalSinceReferenceDate,
                       original.endDate?.timeIntervalSinceReferenceDate,
                       accuracy: 0.001)
        XCTAssertEqual(decoded.remainingSeconds, original.remainingSeconds)
        XCTAssertEqual(decoded.sessionNumber, original.sessionNumber)
        XCTAssertEqual(decoded.completedPomodoros, original.completedPomodoros)
    }

    func test_codable_nilEndDate() throws {
        var state = TimerStateDTO.defaultState
        state.endDate = nil

        let data = try JSONEncoder().encode(state)
        let decoded = try JSONDecoder().decode(TimerStateDTO.self, from: data)

        XCTAssertNil(decoded.endDate)
    }

    // MARK: Equatable

    func test_equatable_sameValues_areEqual() {
        let a = TimerStateDTO.defaultState
        var b = TimerStateDTO.defaultState
        XCTAssertEqual(a, b)

        b.phase = .work
        XCTAssertNotEqual(a, b)
    }
}

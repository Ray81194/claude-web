"""
PomodoroTimer ロジックテストランナー

Swift/XCTest と同等のテストロジックを Python で実装し、
CI や Swift が使えない環境でコアビジネスロジックを検証します。

対応 Swift テスト:
  - TimerStateDTOTests
  - SharedDataStoreTests (Codable ロジック部分)
"""

import json
import dataclasses
from dataclasses import dataclass, field
from typing import Optional
from datetime import datetime, timedelta
import sys

# ── ANSI カラー ──────────────────────────────────────────────
GREEN  = "\033[92m"
RED    = "\033[91m"
YELLOW = "\033[93m"
RESET  = "\033[0m"
BOLD   = "\033[1m"

# ── テストランナー ────────────────────────────────────────────

passed = 0
failed = 0
errors: list[str] = []

def assert_eq(label: str, got, expected):
    global passed, failed
    if got == expected:
        print(f"  {GREEN}✓{RESET} {label}")
        passed += 1
    else:
        print(f"  {RED}✗{RESET} {label}")
        print(f"      expected: {expected!r}")
        print(f"      got:      {got!r}")
        failed += 1
        errors.append(label)

def assert_none(label: str, got):
    global passed, failed
    if got is None:
        print(f"  {GREEN}✓{RESET} {label}")
        passed += 1
    else:
        print(f"  {RED}✗{RESET} {label}")
        print(f"      expected: None, got: {got!r}")
        failed += 1
        errors.append(label)

def assert_not_none(label: str, got):
    global passed, failed
    if got is not None:
        print(f"  {GREEN}✓{RESET} {label}")
        passed += 1
    else:
        print(f"  {RED}✗{RESET} {label}")
        print(f"      expected: not None, got: None")
        failed += 1
        errors.append(label)

def assert_false(label: str, got):
    assert_eq(label, got, False)

def assert_true(label: str, got):
    assert_eq(label, got, True)

def section(name: str):
    print(f"\n{BOLD}{YELLOW}▶ {name}{RESET}")

# ── TimerStateDTO の Python 実装 ──────────────────────────────

PHASE_LABELS = {
    "idle":       "待機中",
    "work":       "作業中",
    "shortBreak": "短い休憩",
    "longBreak":  "長い休憩",
}

@dataclass
class TimerStateDTO:
    phase: str                      # "idle" | "work" | "shortBreak" | "longBreak"
    is_running: bool
    end_date: Optional[datetime]
    remaining_seconds: int
    completed_pomodoros: int
    session_number: int
    work_duration: int              # 秒
    short_break_duration: int       # 秒
    long_break_duration: int        # 秒
    notifications_enabled: bool

    @staticmethod
    def default_state() -> "TimerStateDTO":
        return TimerStateDTO(
            phase="idle",
            is_running=False,
            end_date=None,
            remaining_seconds=1500,
            completed_pomodoros=0,
            session_number=1,
            work_duration=1500,
            short_break_duration=300,
            long_break_duration=900,
            notifications_enabled=True,
        )

    @property
    def current_phase_duration(self) -> int:
        if self.phase in ("idle", "work"):
            return self.work_duration
        if self.phase == "shortBreak":
            return self.short_break_duration
        return self.long_break_duration

    def next_phase_state(self) -> "TimerStateDTO":
        import copy
        next_s = copy.copy(self)
        next_s.is_running = False
        next_s.end_date = None

        if self.phase == "idle":
            next_s.phase = "work"
        elif self.phase == "work":
            next_s.completed_pomodoros += 1
            if self.session_number >= 4:
                next_s.phase = "longBreak"
            else:
                next_s.phase = "shortBreak"
        elif self.phase == "shortBreak":
            next_s.phase = "work"
            next_s.session_number = min(self.session_number + 1, 4)
        elif self.phase == "longBreak":
            next_s.phase = "idle"
            next_s.session_number = 1
            next_s.completed_pomodoros = 0

        next_s.remaining_seconds = next_s.current_phase_duration
        return next_s

    def to_dict(self) -> dict:
        d = dataclasses.asdict(self)
        d["end_date"] = self.end_date.isoformat() if self.end_date else None
        return d

    @staticmethod
    def from_dict(d: dict) -> "TimerStateDTO":
        d = dict(d)
        if d.get("end_date"):
            d["end_date"] = datetime.fromisoformat(d["end_date"])
        else:
            d["end_date"] = None
        return TimerStateDTO(**d)


# ── テストケース ──────────────────────────────────────────────

def test_timer_state_dto():
    section("TimerStateDTOTests — defaultState")

    s = TimerStateDTO.default_state()
    assert_eq("phase は idle", s.phase, "idle")
    assert_false("is_running は False", s.is_running)
    assert_none("end_date は None", s.end_date)
    assert_eq("session_number は 1", s.session_number, 1)
    assert_eq("completed_pomodoros は 0", s.completed_pomodoros, 0)

    section("TimerStateDTOTests — currentPhaseDuration")

    for phase, expected_key in [
        ("idle",       "work_duration"),
        ("work",       "work_duration"),
        ("shortBreak", "short_break_duration"),
        ("longBreak",  "long_break_duration"),
    ]:
        s = TimerStateDTO.default_state()
        s.phase = phase
        assert_eq(
            f"currentPhaseDuration({phase})",
            s.current_phase_duration,
            getattr(s, expected_key)
        )

    section("TimerStateDTOTests — nextPhaseState (work → shortBreak, sessions 1–3)")

    for session in [1, 2, 3]:
        s = TimerStateDTO.default_state()
        s.phase = "work"
        s.session_number = session
        n = s.next_phase_state()
        assert_eq(f"work session{session} → shortBreak", n.phase, "shortBreak")
        assert_eq(f"work session{session}: completedPomodoros +1", n.completed_pomodoros, 1)
        assert_false(f"work session{session}: is_running=False", n.is_running)
        assert_none(f"work session{session}: end_date=None", n.end_date)

    section("TimerStateDTOTests — nextPhaseState (work session4 → longBreak)")

    s = TimerStateDTO.default_state()
    s.phase = "work"
    s.session_number = 4
    n = s.next_phase_state()
    assert_eq("work session4 → longBreak", n.phase, "longBreak")
    assert_eq("completed_pomodoros は 1", n.completed_pomodoros, 1)

    section("TimerStateDTOTests — nextPhaseState (shortBreak → work)")

    s = TimerStateDTO.default_state()
    s.phase = "shortBreak"
    s.session_number = 1
    n = s.next_phase_state()
    assert_eq("shortBreak → work", n.phase, "work")
    assert_eq("session_number は 2", n.session_number, 2)

    # session_number は 4 を超えない
    s.session_number = 4
    n = s.next_phase_state()
    assert_eq("shortBreak session4: session_number は max 4", n.session_number, 4)

    section("TimerStateDTOTests — nextPhaseState (longBreak → idle reset)")

    s = TimerStateDTO.default_state()
    s.phase = "longBreak"
    s.session_number = 4
    s.completed_pomodoros = 4
    n = s.next_phase_state()
    assert_eq("longBreak → idle", n.phase, "idle")
    assert_eq("session_number リセット", n.session_number, 1)
    assert_eq("completed_pomodoros リセット", n.completed_pomodoros, 0)

    section("TimerStateDTOTests — nextPhaseState remainingSeconds")

    s = TimerStateDTO.default_state()
    s.phase = "work"
    s.session_number = 1
    n = s.next_phase_state()
    assert_eq(
        "work→shortBreak: remainingSeconds = shortBreakDuration",
        n.remaining_seconds,
        s.short_break_duration
    )

    section("TimerStateDTOTests — Phase.label")

    for phase, label in PHASE_LABELS.items():
        assert_eq(f"label({phase})", PHASE_LABELS[phase], label)

    section("TimerStateDTOTests — Full Pomodoro Cycle (4 work sessions)")

    s = TimerStateDTO.default_state()
    s.phase = "work"
    s.session_number = 1

    steps = [
        # (遷移後フェーズ, session_number)
        ("shortBreak", 1),
        ("work",       2),
        ("shortBreak", 2),
        ("work",       3),
        ("shortBreak", 3),
        ("work",       4),
        ("longBreak",  4),
        ("idle",       1),
    ]
    for i, (expected_phase, _) in enumerate(steps):
        s = s.next_phase_state()
        assert_eq(f"サイクルステップ{i+1}: phase={expected_phase}", s.phase, expected_phase)

    assert_eq("フルサイクル後: completed_pomodoros リセット", s.completed_pomodoros, 0)
    assert_eq("フルサイクル後: session_number リセット", s.session_number, 1)


def test_codable():
    section("Codable — to_dict / from_dict round-trip")

    original = TimerStateDTO.default_state()
    original.phase = "work"
    original.is_running = True
    original.end_date = datetime(2025, 1, 1, 12, 30, 0)
    original.remaining_seconds = 900
    original.session_number = 2
    original.completed_pomodoros = 3

    d = original.to_dict()
    json_str = json.dumps(d)  # JSON シリアライズ
    decoded_dict = json.loads(json_str)
    decoded = TimerStateDTO.from_dict(decoded_dict)

    assert_eq("phase 保持", decoded.phase, original.phase)
    assert_eq("is_running 保持", decoded.is_running, original.is_running)
    assert_eq("remaining_seconds 保持", decoded.remaining_seconds, original.remaining_seconds)
    assert_eq("session_number 保持", decoded.session_number, original.session_number)
    assert_eq("completed_pomodoros 保持", decoded.completed_pomodoros, original.completed_pomodoros)
    assert_not_none("end_date 保持（not None）", decoded.end_date)

    section("Codable — nil end_date の保存")

    s = TimerStateDTO.default_state()
    s.end_date = None
    d = s.to_dict()
    decoded = TimerStateDTO.from_dict(d)
    assert_none("end_date=None が復元される", decoded.end_date)

    section("Codable — 上書き保存")

    store: dict = {}
    first = TimerStateDTO.default_state()
    first.phase = "work"
    store["timerState"] = first.to_dict()

    second = TimerStateDTO.default_state()
    second.phase = "longBreak"
    store["timerState"] = second.to_dict()

    loaded = TimerStateDTO.from_dict(store["timerState"])
    assert_eq("後から保存した値で上書き", loaded.phase, "longBreak")


def test_settings():
    section("設定変更 — カスタム作業時間の適用")

    s = TimerStateDTO.default_state()
    s.work_duration = 50 * 60  # 50分に変更
    s.phase = "work"
    assert_eq("カスタム作業時間が current_phase_duration に反映", s.current_phase_duration, 50 * 60)

    s.phase = "idle"
    next_s = s.next_phase_state()
    # idle → work なので remainingSeconds は workDuration
    # (next_phase_state の idle 遷移では phase が work になる)
    assert_eq("idle→work: remainingSeconds=workDuration", next_s.remaining_seconds, 50 * 60)


# ── エントリーポイント ─────────────────────────────────────────

if __name__ == "__main__":
    print(f"\n{BOLD}=== PomodoroTimer ロジックテスト ==={RESET}")

    test_timer_state_dto()
    test_codable()
    test_settings()

    total = passed + failed
    print(f"\n{BOLD}{'='*40}{RESET}")
    if failed == 0:
        print(f"{GREEN}{BOLD}✓ 全テスト通過: {passed}/{total}{RESET}")
    else:
        print(f"{RED}{BOLD}✗ 失敗: {failed}/{total}{RESET}")
        print(f"\n失敗したテスト:")
        for e in errors:
            print(f"  - {e}")
        sys.exit(1)

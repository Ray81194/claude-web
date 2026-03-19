# PomodoroTimer — iPad Widget App

iPadのホーム画面・ロック画面で操作できるポモドーロタイマーアプリです。

## 機能

| 機能 | 詳細 |
|---|---|
| ウィジェット（Medium） | カウントダウン表示 + 再生/停止/リセットボタン |
| ウィジェット（Small） | カウントダウン表示 + フェーズ名 |
| ウィジェット（ロック画面） | 円形ゲージ / 矩形テキスト |
| アプリ本体 | タイマー設定（作業時間・休憩時間・通知） |
| 通知 | フェーズ終了時のローカル通知 |

## 技術スタック

- **Swift + SwiftUI** — 全UI
- **WidgetKit** — ウィジェット表示・更新
- **App Intents** — ウィジェット内ボタン操作（iOS 17+ 必須）
- **UNUserNotificationCenter** — ローカル通知
- **App Groups + UserDefaults** — アプリ↔ウィジェット間データ共有

## 必要環境

- Xcode 15 以上
- iOS / iPadOS **17.0** 以上
- 実機またはシミュレーター（iPad Air / iPad Pro 推奨）

## セットアップ手順

### 1. Xcodeプロジェクト作成

1. Xcode で `File > New > Project > iOS App` を選択
2. Product Name: `PomodoroTimer`
3. Bundle Identifier: `com.yourcompany.pomodorotimer`

### 2. ウィジェット拡張ターゲット追加

1. `File > New > Target > Widget Extension`
2. Product Name: `PomodoroTimerWidget`
3. Bundle Identifier: `com.yourcompany.pomodorotimer.widget`
4. 「Include Configuration Intent」のチェックを **外す**（StaticConfiguration を使用）

### 3. App Groups の設定（必須）

両ターゲット（アプリ + ウィジェット）に同じ App Group を追加する：

1. プロジェクト設定 > **PomodoroTimer** ターゲット > `Signing & Capabilities`
2. `+ Capability > App Groups` を追加
3. `group.com.yourcompany.pomodorotimer` を追加（または任意のIDで作成）
4. **PomodoroTimerWidget** ターゲットにも同じ操作を繰り返す
5. `Shared/SharedConstants.swift` の `appGroupID` を同じ値に更新

### 4. ソースファイルの追加

このリポジトリのファイルを以下のようにターゲットへ追加：

| ファイル | 追加先ターゲット |
|---|---|
| `Shared/*.swift` | PomodoroTimer **と** PomodoroTimerWidget（両方） |
| `PomodoroTimer/App/*.swift` | PomodoroTimer |
| `PomodoroTimer/Views/*.swift` | PomodoroTimer |
| `PomodoroTimer/Services/*.swift` | PomodoroTimer |
| `PomodoroTimerWidget/*.swift` | PomodoroTimerWidget |
| `PomodoroTimerWidget/Views/*.swift` | PomodoroTimerWidget |

### 5. Info.plist への追加

`PomodoroTimer/Info.plist` に以下を追加：

```xml
<key>NSUserNotificationUsageDescription</key>
<string>タイマー終了をお知らせします</string>
```

### 6. ビルドと実行

```
Xcode > Run (⌘R)
```

## プロジェクト構造

```
PomodoroTimer/
├── Shared/
│   ├── SharedConstants.swift      # App Group ID / UserDefaultsキー
│   ├── TimerStateDTO.swift        # 共有データ構造（Codable）
│   └── SharedDataStore.swift      # App Group読み書き
│
├── PomodoroTimer/                 # メインアプリ（設定画面）
│   ├── App/
│   │   └── PomodoroTimerApp.swift
│   ├── Views/
│   │   └── SettingsView.swift
│   └── Services/
│       ├── NotificationService.swift
│       └── TimerIntents.swift     # App Intents定義
│
└── PomodoroTimerWidget/           # ウィジェット拡張
    ├── PomodoroTimerWidget.swift
    ├── PomodoroTimelineProvider.swift
    ├── WidgetTimerEntry.swift
    └── Views/
        ├── WidgetSmallView.swift
        ├── WidgetMediumView.swift
        └── WidgetLockScreenView.swift
```

## ポモドーロサイクル

```
待機 → 作業(25分) → 短い休憩(5分) → 作業(25分) → ...（4回）→ 長い休憩(15分) → 待機
```

## カスタマイズ

`SharedConstants.swift` の `appGroupID` を自分のBundleIDに合わせて変更してください。

作業時間・休憩時間はアプリ内の設定画面から変更できます。

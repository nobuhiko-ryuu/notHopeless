# アーキテクチャ設計書（MVP）世の中捨てたもんじゃない

## 1. システム全体構成

```
┌─────────────────────────────────────────────────────┐
│                  Android アプリ                      │
│  Jetpack Compose UI                                 │
│  ─────────────────────────────────────────────────  │
│  ViewModel  ←→  Repository  ←→  Firebase SDK        │
│                              ←→  DataStore（ローカル）│
└──────────────────┬──────────────────────────────────┘
                   │
       ┌───────────┼──────────────────┐
       │           │                  │
       ▼           ▼                  ▼
┌──────────┐ ┌─────────────┐  ┌───────────────┐
│Firestore │ │Cloud        │  │Firebase       │
│（読む）  │ │Functions    │  │Auth（匿名）   │
│          │ │（書く）     │  └───────────────┘
└──────────┘ └──────┬──────┘
                    │ 書き込み
                    ▼
             ┌──────────┐
             │Firestore │
             │（更新）  │
             └──────────┘
```

---

## 2. 技術スタック

### 2.1 Androidアプリ

| カテゴリ | 採用技術 | 理由 |
|---|---|---|
| 言語 | Kotlin | Android標準 |
| UI | Jetpack Compose | 宣言的UI、テスト容易性 |
| アーキテクチャ | MVVM + Repository パターン | 関心分離、テスト容易性 |
| DI | Hilt | Googleサポート、Composeとの親和性 |
| ナビゲーション | Navigation Compose | Compose対応 |
| 非同期 | Coroutines + Flow | Kotlin標準、Firestore対応 |
| ローカル永続化 | Jetpack DataStore（Preferences） | オンボーディング完了フラグの保存 |
| 認証 | Firebase Authentication SDK | 匿名認証 |
| DB読み取り | Firebase Firestore SDK | セキュリティルールで visible のみ |
| Functions呼び出し | Firebase Functions SDK | 書き込み経路の統一 |
| 広告 | Google AdMob SDK | Google App Campaigns との連動 |
| 分析 | Firebase Analytics SDK | 計測イベント送信 |
| クラッシュ監視 | Firebase Crashlytics SDK | Crash-free 99.7%+ 要件 |

### 2.2 バックエンド（Firebase/GCP）

| サービス | 用途 |
|---|---|
| Firebase Authentication | 匿名認証（UID発行） |
| Cloud Firestore | メインDB |
| Cloud Functions for Firebase（Node.js 20） | 書き込みロジック・検証・スケジューラ |
| Firebase Analytics | 行動イベント計測 |
| Firebase Crashlytics | クラッシュ収集 |
| Google AdMob | 広告配信 |

---

## 3. Androidアプリ レイヤー構成

```
┌──────────────────────────────────────────────┐
│  UI Layer                                    │
│  Screen Composables + ViewModel              │
│  （画面状態の管理・ユーザー操作の受付）       │
├──────────────────────────────────────────────┤
│  Data Layer                                  │
│  Repository（インターフェース + 実装）        │
│  Remote（Firestore / Functions）             │
│  Local（DataStore）                          │
└──────────────────────────────────────────────┘
```

### 3.1 パッケージ構成（案）

```
com.notHopeless.app/
├── ui/
│   ├── onboarding/     OnboardingScreen, OnboardingViewModel
│   ├── home/           HomeScreen, HomeViewModel
│   ├── post/           PostScreen, PostViewModel
│   ├── my/             MyScreen, MyViewModel
│   ├── guidelines/     GuidelinesScreen（ViewModel不要）
│   ├── report/         ReportScreen, ReportViewModel
│   └── common/         共通UI部品（PostCard, LoadingView, ErrorView, AdCard）
├── data/
│   ├── repository/     PostRepository, ReactionRepository,
│   │                   ReportRepository, UserRepository, SettingsRepository
│   ├── remote/         FirestoreDataSource, FunctionsDataSource
│   ├── local/          DataStoreDataSource
│   └── model/          Post, DailyPick, Reaction, Report, UserSettings
├── di/
│   └── AppModule.kt    Hilt モジュール
└── MainActivity.kt
```

---

## 4. 画面・ViewModel 対応表

| 画面 | ViewModel | 主な責務 |
|---|---|---|
| OnboardingScreen | OnboardingViewModel | ページ遷移・完了フラグ保存 |
| HomeScreen | HomeViewModel | フィード取得・デイリーピックス取得・反応処理 |
| PostScreen | PostViewModel | フォーム状態管理・バリデーション・送信 |
| MyScreen | MyViewModel | 自分の投稿一覧取得 |
| GuidelinesScreen | なし | 静的テキスト表示 |
| ReportScreen | ReportViewModel | 通報送信 |

---

## 5. データフロー

### 5.1 フィード閲覧（読む）

```
HomeScreen
  └→ HomeViewModel.loadFeed()
       └→ PostRepository.getFeed()
            └→ Firestore SDK
                 └→ posts（status==visible, createdAt desc, cursor paging）
```

### 5.2 デイリーピックス（読む）

```
HomeScreen
  └→ HomeViewModel.loadDailyPicks()
       └→ PostRepository.getDailyPicks(date)
            └→ Firestore SDK
                 └→ dailyPicks/{date} → pickIds (3件)
                      └→ posts を postId in [...] でバッチ取得（1クエリ）
```

### 5.3 投稿（書く）

```
PostScreen
  └→ PostViewModel.submit()
       ├→ [クライアント検証] 必須チェック・文字数・固有名詞疑い警告
       └→ PostRepository.createPost(payload)
            └→ Functions SDK → createPost Function
                 ├→ [サーバ検証] 個人情報・固有名詞濃厚 → reject
                 ├→ クールダウンチェック（users/{uid}.lastPostAt）
                 └→ posts コレクションに書き込み
```

### 5.4 反応（楽観更新）

```
HomeScreen（PostCard）
  └→ HomeViewModel.react(postId, type)
       ├→ [即時] ローカルの reactionCounts を +1（UI即時反映）
       ├→ ReactionRepository.react(postId, userId, type)
       │    └→ Functions SDK → reactToPost Function
       │         ├→ reactionId = {postId}_{uid} で reactions の存在確認
       │         │    同 type → スキップ（冪等）
       │         │    別 type → 旧カウント -1・新カウント +1 で上書き（トランザクション）
       │         │    存在なし → reactions に追加 & reactionCounts[type] +1
       │         └→ 成功レスポンス返却
       └→ 失敗時: ローカルを元に戻す + Snackbar
```

### 5.5 通報（書く）

```
HomeScreen（⋯メニュー）→ ReportScreen
  └→ ReportViewModel.submit(postId, reason)
       └→ ReportRepository.report(postId, reporterId, reason)
            └→ Functions SDK → reportPost Function
                 └→ reports コレクションに書き込み
```

---

## 6. Firestore コレクション構成

```
posts/{postId}                  投稿
reactions/{reactionId}          反応（二重押し防止・カウント補助）
reports/{reportId}              通報
dailyPicks/{date}               デイリーピックス（Functions生成）
stocks/{stockId}                運営ストック（例文・管理者投入）
users/{uid}                     ユーザー（クールダウン管理）
```

### 6.1 Security Rules 方針

| コレクション | 読み取り | 書き込み |
|---|---|---|
| posts | 認証済みユーザー（visible のみ、または自分の投稿） | Functions のみ（service account） |
| reactions | Functions のみ | Functions のみ |
| reports | Functions のみ | Functions のみ |
| dailyPicks | 認証済みユーザー | Functions のみ |
| stocks | Functions のみ | 管理者のみ（Admin SDK） |
| users | 本人のみ（lastPostAt等） | Functions のみ |

---

## 7. Cloud Functions 一覧

| Function名 | トリガー | 概要 |
|---|---|---|
| createPost | HTTPS Callable | 投稿作成（入力正規化・検証・クールダウン・保存） |
| reactToPost | HTTPS Callable | 反応（二重押し防止・カウント更新） |
| reportPost | HTTPS Callable | 通報受付（保存） |
| buildDailyPicks | Cloud Scheduler（毎日 JST 06:00） | デイリーピックス生成 |

---

## 8. 広告構成（AdMob）

- **広告フォーマット**：ネイティブ広告（Native Ad）
  - 理由：本文に被せない要件、LazyColumn に自然に挿入できる
- **配置ルール**
  - 今日の3つセクションの直後：1枠
  - フィード：10投稿に1回（0ベースインデックス 10, 21, 32…、つまり11番目・22番目・33番目…の位置に挿入）
- **禁止事項**：本文へのオーバーラップ、スクロール中の見切れ、UIの破壊

---

## 9. 分析・監視

### 9.1 Firebase Analytics

以下のカスタムイベントを実装する（詳細は詳細設計書 §8 参照）：

```
install / first_open / view_feed / view_post_form
submit_post_success / submit_post_fail / reaction_click / report_submit / ad_impression
```

### 9.2 Firebase Crashlytics

- アプリ起動時に初期化
- 致命的クラッシュ・非致命的エラーを自動収集
- D0-2 の致命不具合対応に活用

---

## 10. 非機能要件への対応方針

| 要件 | 対応方針 |
|---|---|
| Crash-free 99.7%+ | Crashlytics 監視・全画面に Error 状態と再試行を実装 |
| N+1 禁止 | フィードは1クエリ・デイリーピックスは `in` クエリでバッチ取得 |
| postId 基準（index禁止） | reactions/reports に postId フィールドを保持・Firestore インデックスを適切に設定 |
| NG検知はサーバ最終 | Functions で最終検証を必ず実施、クライアントは UX ガイド補助のみ |
| 広告が本文に被らない | ネイティブ広告を独立アイテムとして配置（Composable レベルで分離） |

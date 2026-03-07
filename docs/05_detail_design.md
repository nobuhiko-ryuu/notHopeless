# 詳細設計書（MVP）世の中捨てたもんじゃない

---

## 1. データモデル詳細

### 1.1 posts/{postId}

| フィールド | 型 | 必須 | 内容 |
|---|---|---|---|
| postId | string（ドキュメントID） | ✅ | auto-generated |
| authorId | string | ✅ | Firebase 匿名 UID |
| scene | string（enum） | ✅ | `commute` / `shop` / `workplace` / `public` |
| kindnessType | string（enum） | ✅ | `care` / `help` / `integrity` / `courage` / `pro` |
| userState | string（enum） | — | `tired` / `rushed` / `down` / `normal` / null |
| effect | string（enum） | ✅ | `relieved` / `lighter` / `inspired` / `survived` / `notHopeless` / `trust` |
| body | string | ✅ | 最大140字 |
| reactionCounts | map | ✅ | `{ notHopeless: 0, moved: 0, doToo: 0 }` |
| isStock | boolean | ✅ | 運営ストックの場合 true（フロントでの表示は同一） |
| createdAt | Timestamp | ✅ | サーバタイムスタンプ |
| status | string（enum） | ✅ | `visible` / `hidden` |

**Firestoreインデックス**（複合）：
- `status ASC, createdAt DESC`（フィード取得用）
- `authorId ASC, createdAt DESC`（My画面用）

---

### 1.2 reactions/{reactionId}

| フィールド | 型 | 必須 | 内容 |
|---|---|---|---|
| reactionId | string（ドキュメントID） | ✅ | `{postId}_{userId}` 固定形式（ユニーク確保） |
| postId | string | ✅ | 対象投稿ID |
| userId | string | ✅ | Firebase 匿名 UID |
| type | string（enum） | ✅ | `notHopeless` / `moved` / `doToo` |
| createdAt | Timestamp | ✅ | サーバタイムスタンプ |

> **ユニーク制約**: `reactionId = {postId}_{userId}` とすることで Firestore レベルで1ユーザー1回を保証する。
> 型変更（押し直し）は MVP スコープ外：再送時は同ドキュメントを上書き（type を更新）。

---

### 1.3 reports/{reportId}

| フィールド | 型 | 必須 | 内容 |
|---|---|---|---|
| reportId | string（ドキュメントID） | ✅ | auto-generated |
| postId | string | ✅ | 通報対象の投稿ID |
| reporterId | string | ✅ | Firebase 匿名 UID |
| reason | string（enum） | ✅ | `personal_info` / `harassment` / `discrimination` / `sexual` / `other` |
| createdAt | Timestamp | ✅ | サーバタイムスタンプ |

**Firestoreインデックス**：
- `postId ASC`（管理者が投稿別通報数を確認するため）

---

### 1.4 dailyPicks/{date}

| フィールド | 型 | 必須 | 内容 |
|---|---|---|---|
| date | string（ドキュメントID） | ✅ | `YYYY-MM-DD` 形式・**JST基準**（例: `2026-03-07`） |
| pickIds | array\<string\> | ✅ | postId の配列（3件） |
| generatedAt | Timestamp | ✅ | 生成タイムスタンプ |

---

### 1.5 stocks/{stockId}

| フィールド | 型 | 必須 | 内容 |
|---|---|---|---|
| stockId | string（ドキュメントID） | ✅ | auto-generated |
| scene | string（enum） | ✅ | posts と同じ enum |
| kindnessType | string（enum） | ✅ | 同上 |
| effect | string（enum） | ✅ | 同上 |
| body | string | ✅ | 例文本文 |
| usedAt | Timestamp | — | 最後にデイリーピックスに使われた日時（重複使用の抑制） |
| createdAt | Timestamp | ✅ | |

> **初期データ投入方針**: stocks は運営が Admin SDK 経由（または Firestore コンソール）で手動投入する。
> リリース前に最低10件以上用意し、初期の「今日の3つ」が空にならないよう保証する。

---

### 1.6 users/{uid}

| フィールド | 型 | 必須 | 内容 |
|---|---|---|---|
| uid | string（ドキュメントID） | ✅ | Firebase 匿名 UID |
| lastPostAt | Timestamp | — | 最後に投稿した日時（クールダウン計算用） |
| createdAt | Timestamp | ✅ | アカウント初回作成日時 |

---

## 2. Firestore Security Rules（方針レベル）

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // 認証チェック
    function isAuth() {
      return request.auth != null;
    }

    // posts: 読み = visible のみ（ただし自分の投稿は hidden も読める） / 書き = Functions のみ
    match /posts/{postId} {
      allow read: if isAuth() && (
        resource.data.status == 'visible' ||
        resource.data.authorId == request.auth.uid  // My画面で自分の hidden 投稿を表示するため
      );
      allow write: if false; // Functions (Admin SDK) のみ
    }

    // dailyPicks: 読み = 認証済み / 書き = Functions のみ
    match /dailyPicks/{date} {
      allow read: if isAuth();
      allow write: if false;
    }

    // reactions / reports / users: クライアントから直接操作禁止
    match /reactions/{id}  { allow read, write: if false; }
    match /reports/{id}    { allow read, write: if false; }
    match /stocks/{id}     { allow read, write: if false; }
    match /users/{uid}     { allow read, write: if false; }
  }
}
```

---

## 3. Cloud Functions 詳細

### 3.1 createPost

**入力（Callable payload）**

```typescript
{
  scene: string,         // enum
  kindnessType: string,  // enum
  userState?: string,    // enum | null
  effect: string,        // enum
  body: string,          // max 140字
}
```

**処理フロー**

```
1. 認証チェック（uid 取得）
2. 入力正規化
   - 全角/半角の統一、ゼロ幅文字除去、前後空白トリム
3. バリデーション
   a. 必須フィールドの存在確認
   b. enum 値の合法性チェック
   c. body: 140字超過 → reject
   d. body: 個人情報パターン（正規表現）→ reject（エラーコード: PERSONAL_INFO）
   e. body: 固有名詞濃厚パターン → reject（エラーコード: SPECIFIC_NOUN）
4. クールダウンチェック
   - users/{uid}.lastPostAt が 現在時刻 - 5分 以内 → reject（エラーコード: COOLDOWN）
5. Firestore 書き込み
   - posts/{auto-id} を作成（status: 'visible', reactionCounts: {notHopeless:0, moved:0, doToo:0}）
   - users/{uid}.lastPostAt を現在時刻で更新
6. 成功レスポンス: { postId }
```

**固有名詞・個人情報チェック用パターン（MVP初期）**

| カテゴリ | パターン例 | 処理 |
|---|---|---|
| 電話番号 | `\d{2,4}[-\s]\d{2,4}[-\s]\d{4}` | reject |
| メールアドレス | `[^\s]+@[^\s]+\.[^\s]+` | reject |
| URL/SNS ID | `https?://`, `@[A-Za-z0-9_]+` | reject |
| 郵便番号/住所 | `〒\d{3}-\d{4}`, `[0-9]+丁目` | reject |
| 固有名詞濃厚 | 「〜駅」「〜店」「〜さん」「〜学校」「〜会社」等のサフィックス検出 | reject |

> 注意：過検出（誤reject）のリスクがあるため、リリース後に実データで調整する。
> 固有名詞チェックの精度は運用しながら段階的に改善する。

---

### 3.2 reactToPost

**入力**

```typescript
{ postId: string, type: 'notHopeless' | 'moved' | 'doToo' }
```

**処理フロー**

```
1. 認証チェック（uid 取得）
2. reactionId = `{postId}_{uid}` を生成
3. reactions/{reactionId} の存在確認
   - 存在しない: reactions に書き込み & posts/{postId}.reactionCounts[type] +1
   - 存在する（同じ type）: スキップ（冪等）
   - 存在する（別 type）: MVP では上書き更新（旧カウント -1、新カウント +1）
4. 成功レスポンス: {}
```

> Firestore トランザクションを使いカウントの整合性を確保する。

---

### 3.3 reportPost

**入力**

```typescript
{
  postId: string,
  reason: 'personal_info' | 'harassment' | 'discrimination' | 'sexual' | 'other'
}
```

**処理フロー**

```
1. 認証チェック（uid 取得）
2. posts/{postId} の存在確認（存在しない場合は reject）
3. reports/{auto-id} に書き込み
4. 成功レスポンス: {}
```

> 同一ユーザーが同一投稿を複数回通報することを MVP では許容（運営側でフィルタ）。

---

### 3.4 buildDailyPicks（Cloud Scheduler）

**トリガー**：Cloud Scheduler → Pub/Sub → Cloud Functions
**実行時刻**：毎日 JST 06:00（cron: `0 21 * * *` UTC）

**処理フロー**

```
1. 本日付（JST）の date = "YYYY-MM-DD" を生成
2. 既に dailyPicks/{date} が存在する場合はスキップ（冪等）
3. 候補リスト構築
   a. 直近24時間の posts から reactionCounts 合計の上位を取得（最大10件）
   b. 直近72時間の posts から新着順に補完（重複除く）
4. 候補が3件未満の場合
   - stocks から usedAt が古い順に補完
   - 補完したストックに usedAt = 現在時刻 を書き込み
   - ストックでも不足の場合はそのまま（最大3件）
5. dailyPicks/{date} を書き込み
   { date, pickIds: [...最大3件...], generatedAt }
```

---

## 4. 画面詳細

### 4.1 Onboarding

**状態管理**

```
data class OnboardingUiState(
  val currentPage: Int = 0,  // 0, 1, 2
  val isCompleted: Boolean = false
)
```

**ViewModel アクション**

- `nextPage()`: currentPage + 1（page 2 の場合は complete()）
- `complete()`: DataStore に onboardingCompleted = true を保存 → Home へナビゲート

**完了チェック（MainActivity or NavGraph）**

- アプリ起動時に DataStore の `onboardingCompleted` を確認
- false → Onboarding へ、true → Home へ

---

### 4.2 Home

**状態管理**

```
data class HomeUiState(
  val dailyPicks: List<Post> = emptyList(),
  val feed: List<FeedItem> = emptyList(),  // PostCard or AdCard
  val isLoading: Boolean = false,
  val hasError: Boolean = false,
  val nextCursor: Timestamp? = null,
  val isLoadingMore: Boolean = false,
  val reactionState: Map<String, ReactionType?> = emptyMap()  // postId -> 押した type
)
sealed class FeedItem {
  data class PostCard(val post: Post) : FeedItem()
  object AdCard : FeedItem()
}
```

**フィード構築ロジック（ViewModel）**

```
フィード raw リスト（posts）に対して:
- インデックス 10, 21, 32, ... の位置（11番目, 22番目...）に AdCard を挿入
- dailyPicks セクションは FeedItem 外（別 LazyColumn セクション）
```

**ページング**

- 初回: 20件取得（`createdAt DESC`, `status == visible`）
- スクロール末端到達: 次の20件（cursor = 末尾の createdAt）
- 再試行: `retry()` で再取得

**反応状態の管理**

- `reactionState` でローカルの「押した反応」を管理
- Functions 呼び出し失敗時はローカルを元に戻す

---

### 4.3 Post（投稿フォーム）

**状態管理**

```
data class PostUiState(
  val scene: SceneType? = null,
  val kindnessType: KindnessType? = null,
  val userState: UserStateType? = null,
  val effect: EffectType? = null,
  val body: String = "",
  val bodyLength: Int = 0,
  val hasProperNounWarning: Boolean = false,
  val isSubmitting: Boolean = false,
  val submitError: SubmitError? = null
)
enum class SubmitError { PERSONAL_INFO, SPECIFIC_NOUN, COOLDOWN, NETWORK, UNKNOWN }
```

**バリデーション（クライアント）**

| チェック | タイミング | 結果 |
|---|---|---|
| 必須フィールド未入力 | 送信ボタン押下時 | 送信ボタン非活性（常時） |
| body 140字超過 | 入力中リアルタイム | カウンター赤色・送信不可 |
| 固有名詞"疑い" | 入力中（debounce 1秒） | 警告表示（送信は可） |

**固有名詞"疑い"クライアント判定（簡易）**

```
サフィックス: 駅, 店, 会社, 学校, 病院, 公園, さん, くん, ちゃん, 様
→ これらを含む場合: hasProperNounWarning = true → 警告バナー表示
```

**警告文**：
> 安心のため、店名・駅名・人が特定できる情報は控えてね。ぼかして書き直してみよう。

**送信ボタン活性条件**

```
scene != null && kindnessType != null && effect != null
&& body.isNotBlank() && body.length <= 140
&& !isSubmitting
```

**エラー表示（送信失敗）**

| SubmitError | 表示メッセージ |
|---|---|
| PERSONAL_INFO | 個人情報が含まれているため送信できませんでした |
| SPECIFIC_NOUN | 特定できる情報が含まれているため送信できませんでした |
| COOLDOWN | 少し時間をおいてから投稿してください（5分間隔） |
| NETWORK | 通信エラーが発生しました。もう一度試してください |
| UNKNOWN | 送信できませんでした。もう一度試してください |

---

### 4.4 My（自分の投稿一覧）

**状態管理**

```
data class MyUiState(
  val posts: List<Post> = emptyList(),
  val isLoading: Boolean = false,
  val hasError: Boolean = false
)
```

- クエリ：`authorId == uid && createdAt DESC`（最大50件、MVP はページングなし）
  - Security Rules で `authorId == request.auth.uid` の場合は hidden 投稿も読み取り可能（フィードとは別条件）
- hidden の投稿：カード上部に「この投稿は非表示になりました」バナーを表示。本文・タグは表示するが反応ボタンは非表示（reactionCounts も表示しない）
- 空状態：「まだ投稿がありません。最初の投稿をしてみよう」

---

### 4.5 Guidelines（投稿ルール）

静的テキスト表示。ViewModel 不要。

**表示内容**
1. 受け取った優しさだけ投稿できます
2. 画像・コメントはありません
3. 店名・駅名・会社名・人名など特定できる情報は書かないでください
4. 個人情報（電話番号・メールアドレス等）は禁止です
5. ルール違反の投稿は非表示・投稿不可になる場合があります
6. 気になる投稿は通報ボタンからご連絡ください

---

### 4.6 Report（通報）

**遷移方法**：Home/My の投稿カード内 `⋯（メニュー）` → BottomSheet（モーダル）で表示
> MVP はフル画面遷移ではなくモーダル（BottomSheet）を採用

**状態管理**

```
data class ReportUiState(
  val postId: String,
  val selectedReason: ReportReason? = null,
  val isSubmitting: Boolean = false,
  val isSuccess: Boolean = false,
  val hasError: Boolean = false
)
enum class ReportReason {
  PERSONAL_INFO, HARASSMENT, DISCRIMINATION, SEXUAL, OTHER
}
```

**理由の日本語表示**

| enum | 表示文言 |
|---|---|
| PERSONAL_INFO | 個人情報・特定できる情報が含まれている |
| HARASSMENT | 誹謗中傷・悪口 |
| DISCRIMINATION | 差別的な内容 |
| SEXUAL | 性的な内容 |
| OTHER | その他 |

**完了後**：BottomSheet を閉じる＋「通報しました。ご協力ありがとうございます」Snackbar

---

## 5. ナビゲーション詳細

```
NavGraph:
  startDestination = if (onboardingCompleted) Home else Onboarding

  Onboarding  → Home（完了時）
  Home        ←→ Post（BottomTab）
  Home        ←→ My（BottomTab）
  Home        → Guidelines（右上アイコン or メニュー）
  Home/My     → Report（BottomSheet：postId を引数に渡す）
```

**BottomTab 構成**

| タブ | アイコン | 画面 |
|---|---|---|
| ホーム | Home | HomeScreen |
| 投稿 | Edit | PostScreen |
| マイ | Person | MyScreen |

---

## 6. enum 値と日本語表示の対応

### scene

| enum値 | 表示文言 |
|---|---|
| commute | 通勤・通学 |
| shop | お店 |
| workplace | 職場 |
| public | 街・公共 |

### kindnessType

| enum値 | 表示文言 |
|---|---|
| care | 気遣い |
| help | 手助け |
| integrity | 誠実 |
| courage | 勇気 |
| pro | プロの仕事 |

### userState

| enum値 | 表示文言 |
|---|---|
| tired | 疲れてた |
| rushed | 焦ってた |
| down | 落ちてた |
| normal | 普通 |

### effect

| enum値 | 表示文言 |
|---|---|
| relieved | 少し安心した |
| lighter | 気持ちが軽くなった |
| inspired | 自分も優しくしようと思った |
| survived | 今日を乗り切れた |
| notHopeless | 捨てたもんじゃないと思った |
| trust | 人を信じてみようと思った |

### reactionType（カード上の反応ボタン）

| enum値 | UI文言 |
|---|---|
| notHopeless | 捨てたもんじゃない |
| moved | 沁みた |
| doToo | 自分もする |

---

## 7. エラーハンドリング方針

| レイヤー | エラー種別 | 処理 |
|---|---|---|
| ViewModel | ネットワーク失敗 | `hasError = true` → UI で再試行ボタン表示 |
| ViewModel | Functions reject | `submitError` を設定 → メッセージ表示 |
| ViewModel | 反応楽観更新失敗 | ローカルカウントを rollback + Snackbar |
| Crashlytics | 予期しない例外 | 自動収集（`logException()` で非致命的ログも送信） |
| Functions | 認証なし | `UNAUTHENTICATED` エラー → アプリ再起動を促す |

---

## 8. 分析イベント詳細（Firebase Analytics）

| イベント名 | 送信タイミング | パラメータ |
|---|---|---|
| `first_open` | 初回起動時 | （自動収集） |
| `onboarding_complete` | オンボーディング完了時 | なし |
| `view_feed` | Home画面表示時 | なし |
| `view_post_form` | Post画面表示時 | なし |
| `submit_post_success` | createPost 成功時 | `scene`, `effect` |
| `submit_post_fail` | createPost 失敗時 | `reason`（エラーコード） |
| `reaction_click` | 反応ボタンタップ時 | `reaction_type`（notHopeless/moved/doToo） |
| `report_submit` | reportPost 送信時 | `reason`（通報理由） |
| `ad_impression` | 広告インプレッション時 | （AdMob ネイティブ広告の `onAdLoaded` コールバックで手動送信） |

---

## 9. 未解決事項（MVP後に判断）

| 項目 | 現状 | 将来検討 |
|---|---|---|
| 固有名詞チェック精度 | 正規表現・サフィックスベース | ML ベースの分類器に移行 |
| モデレーション | 通報を人力確認後 hidden 化 | 閾値自動化（通報数 N件で自動 hidden） |
| 反応の変更（押し直し） | 上書き更新 | 専用 changeReaction Function の追加 |
| クールダウン期間 | 5分 | ユーザーデータを見て調整 |
| ストック切れ | 最大3件に満たない可能性あり | ストック件数の定期監視・補充フロー整備 |
| push通知 | なし | Phase2 での検討 |

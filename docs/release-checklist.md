# リリースチェックリスト

> 作成日: 2026-03-08
> MVP 実装完了後、Google Play ストアへ公開するまでの全手順。

---

## 担当凡例

| マーク | 意味 |
|---|---|
| 👤 | あなたが行う作業（Firebase Console / Google Play Console 等、外部サービスへのアクセスが必要） |
| 🤖 | Claude が行う作業（コード変更） |
| ✅ | 完了チェック欄 |

---

## Phase 1: Firebase プロジェクト構築 👤

### Step 1-1: Firebase プロジェクト作成

1. [Firebase Console](https://console.firebase.google.com/) を開く
2. 「プロジェクトを追加」→ プロジェクト名: `notHopeless`（任意）
3. Google Analytics: **有効**にする（既存の Google アカウントに紐付け）
4. 「プロジェクトを作成」

- [ ] ✅ Firebase プロジェクト作成完了

### Step 1-2: 匿名認証を有効化

1. Firebase Console → **Authentication** → 「始める」
2. 「ログイン方法」タブ → **匿名** → 有効にして保存

- [ ] ✅ 匿名認証 有効化完了

### Step 1-3: Firestore データベース作成

1. Firebase Console → **Firestore Database** → 「データベースの作成」
2. **本番環境モード**を選択（Security Rules は後でデプロイ）
3. ロケーション: **asia-northeast1（東京）**

- [ ] ✅ Firestore 作成完了

### Step 1-4: Android アプリを Firebase に登録

1. Firebase Console → プロジェクト設定（歯車アイコン）→「アプリを追加」→ Android
2. パッケージ名: `com.nothopeless.app`
3. アプリのニックネーム: `notHopeless`
4. 「アプリを登録」→ **`google-services.json` をダウンロード**
5. ダウンロードした `google-services.json` を `android/app/google-services.json` に配置

- [ ] ✅ `google-services.json` 配置完了

### Step 1-5: Firebase CLI インストール & ログイン

```bash
npm install -g firebase-tools
firebase login
cd /path/to/notHopeless
firebase use --add   # プロジェクトを選択
```

- [ ] ✅ Firebase CLI セットアップ完了

### Step 1-6: Firestore rules / indexes / Functions をデプロイ

```bash
# プロジェクトルートで実行

# Firestore rules と indexes
firebase deploy --only firestore

# Cloud Functions（Node.js 20 が必要）
firebase deploy --only functions
```

デプロイ後、Firebase Console → Functions に以下の4関数が表示されていることを確認:
- `createPost`
- `reactToPost`
- `reportPost`
- `buildDailyPicks`

- [ ] ✅ Firestore rules/indexes デプロイ完了
- [ ] ✅ Cloud Functions デプロイ完了

### Step 1-7: stocks コレクションに初期データを投入

Firebase Console → Firestore → 「コレクションを開始」→ コレクションID: `stocks`

以下の構造でドキュメントを**最低10件**追加する:

```
stocks/{auto-id}
  scene:        string   // commute / shop / workplace / public のいずれか
  kindnessType: string   // care / help / integrity / courage / pro のいずれか
  effect:       string   // relieved / lighter / inspired / survived / notHopeless / trust のいずれか
  body:         string   // 投稿本文（140文字以内）
  usedAt:       timestamp  // 2020-01-01T00:00:00Z など古い日付を設定
  createdAt:    timestamp  // 現在日時
```

> **ポイント**: `usedAt` を古い日付にすることで、フォールバック時に最初に選ばれやすくなります。
> 投稿内容は実際の「世の中捨てたもんじゃない」エピソードを用意してください。

各フィールドの有効値:

| フィールド | 有効値 |
|---|---|
| scene | `commute`（通勤通学）/ `shop`（買い物）/ `workplace`（職場）/ `public`（公共の場） |
| kindnessType | `care`（気遣い）/ `help`（手助け）/ `integrity`（誠実さ）/ `courage`（勇気）/ `pro`（プロの技） |
| effect | `relieved`（ほっとした）/ `lighter`（気が楽になった）/ `inspired`（元気をもらった）/ `survived`（助かった）/ `notHopeless`（捨てたもんじゃない）/ `trust`（人を信じられた） |

- [ ] ✅ stocks 初期データ投入完了（最低10件）

---

## Phase 2: AdMob セットアップ 👤

### Step 2-1: AdMob アカウント作成

1. [AdMob](https://admob.google.com/) にアクセス → Google アカウントでログイン
2. 「始める」→ 国・タイムゾーン・通貨を設定 → アカウント作成

- [ ] ✅ AdMob アカウント作成完了

### Step 2-2: アプリを AdMob に登録

1. AdMob Console → **アプリ** → 「アプリを追加」
2. 「アプリはアプリストアに公開されていますか？」→ **いいえ**（リリース前のため）
3. プラットフォーム: **Android** / アプリ名: `世の中捨てたもんじゃない`
4. 「追加」→ **アプリ ID** をメモ

```
AdMob アプリ ID: ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX  ← ここに記入
```

- [ ] ✅ AdMob アプリ登録完了

### Step 2-3: 広告ユニットを作成

1. 追加したアプリ → 「広告ユニットを追加」
2. 広告の形式: **ネイティブ広告**
3. 広告ユニット名: `feed_native`
4. 作成後、**広告ユニット ID** をメモ

```
広告ユニット ID: ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX  ← ここに記入
```

- [ ] ✅ 広告ユニット作成完了

### Step 2-4: AdMob ID をコードに反映 🤖

上記2つの ID を Claude に渡すとコードに反映します（AndroidManifest.xml の差し替え）。

- [ ] ✅ コード反映完了

---

## Phase 3: リリースビルド準備

### Step 3-1: 署名用 keystore を作成 👤

> ⚠️ **重要**: keystore を紛失するとアプリのアップデートが永久にできなくなります。
> 必ずパスワードマネージャー等に安全に保管してください。

```bash
keytool -genkey -v \
  -keystore nothopeless-release.jks \
  -alias nothopeless \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

対話形式で以下を入力:
- **キーストアのパスワード**: 強固なパスワード（16文字以上推奨）
- **名前 (CN)**: 自分の名前またはアプリ名
- **組織単位 (OU)**: （空白でもOK）
- **組織 (O)**: （空白でもOK）
- **都市 (L)**: （空白でもOK）
- **都道府県 (ST)**: （空白でもOK）
- **国コード (C)**: `JP`

完了後、以下をメモして Claude に渡す:

```
keystore ファイルパス: /path/to/nothopeless-release.jks
keystore パスワード:   （パスワードマネージャーに保存）
エイリアス名:          nothopeless
エイリアスパスワード:  （同上）
```

> ⚠️ keystore ファイルと上記情報を Claude（AI）に直接渡さないでください。
> ファイルパスと情報を伝えれば、ローカルで作業できます。

- [ ] ✅ keystore 作成・保存完了

### Step 3-2: 署名設定 + ProGuard ルール追加 🤖

keystore 情報を受け取り次第、以下を対応します:
- `android/app/build.gradle.kts` にリリース署名設定を追加
- `android/app/proguard-rules.pro` に Firebase / Hilt / Compose 用ルールを追加

- [ ] ✅ 署名設定完了

---

## Phase 4: プライバシーポリシー 👤

Google Play 公開・AdMob 利用に必須です。

### Step 4-1: プライバシーポリシーを作成

記載必須の内容:

| 項目 | 内容 |
|---|---|
| 収集する情報 | 匿名ID（Firebase Anonymous Auth）、投稿内容、リアクション履歴 |
| 利用目的 | サービス提供、広告配信（AdMob）、クラッシュ解析（Firebase Crashlytics） |
| 第三者提供 | Google LLC（Firebase / AdMob）へのデータ送信 |
| データ保持・削除 | お問い合わせ窓口（メールアドレス）を記載 |
| 対象年齢 | 13歳未満を対象としない |

[プライバシーポリシー生成ツール（無料）](https://www.freeprivacypolicy.com/) を使うと簡単に作成できます。

- [ ] ✅ プライバシーポリシー作成完了

### Step 4-2: プライバシーポリシーを公開（GitHub Pages 推奨）

1. GitHub リポジトリ → Settings → Pages
2. Source: `Deploy from a branch` / Branch: `master` / Folder: `/ (root)` または `/docs`
3. `docs/privacy-policy.md`（または `.html`）にポリシー内容を書いてプッシュ
4. 数分後に URL が発行される

```
例: https://nobuhiko-ryuu.github.io/notHopeless/privacy-policy
```

- [ ] ✅ プライバシーポリシー公開完了 / URL: ___________________________

### Step 4-3: プライバシーポリシー URL をアプリ内に追加 🤖

URL を受け取り次第、ガイドライン画面またはオンボーディング画面にリンクを追加します。

- [ ] ✅ アプリ内リンク追加完了

---

## Phase 5: Google Play Console 👤

### Step 5-1: デベロッパーアカウント作成

1. [Google Play Console](https://play.google.com/console/) にアクセス
2. デベロッパーアカウントを作成（**登録料: $25、一回限り**）
3. 身分確認・支払い情報を設定（クレジットカード必要）

- [ ] ✅ Play Console アカウント作成完了

### Step 5-2: アプリを作成

1. Play Console → 「アプリを作成」
2. アプリ名: `世の中捨てたもんじゃない`
3. 言語: 日本語 / アプリの種類: アプリ / 無料
4. コンテンツガイドライン・US export laws に同意 → 「アプリを作成」

- [ ] ✅ Play Console アプリ作成完了

### Step 5-3: ストア掲載情報を入力

必要な素材:

| 素材 | サイズ / 文字数 | 内容例 |
|---|---|---|
| アプリアイコン | 512 × 512 px（PNG） | `android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` を書き出し |
| フィーチャーグラフィック | 1024 × 500 px（PNG/JPEG） | アプリのキービジュアル |
| スクリーンショット | 最低2枚（縦向き推奨: 1080 × 1920 px） | ホーム画面・投稿画面・マイ画面 |
| 短い説明 | 80 文字以内 | 「世の中の小さな親切を共有する匿名 SNS」 |
| 詳細な説明 | 4000 文字以内 | アプリの特徴・使い方 |

スクリーンショットの撮り方（Android エミュレーター使用）:
```bash
cd android
./gradlew installDebug   # エミュレーターまたは実機にインストール
# Android Studio の Device Manager でエミュレーターを起動
# Pixel 7 Pro (API 35) 推奨
```

- [ ] ✅ ストア掲載情報 入力完了

### Step 5-4: コンテンツレーティング（IARC）

1. Play Console → アプリ → **コンテンツレーティング** → アンケートを開始
2. カテゴリ: **ソーシャル**
3. 回答方針:
   - 暴力・性的コンテンツ: いいえ
   - ユーザー生成コンテンツ（UGC）: **はい**（投稿機能があるため）
   - 報告/ブロック機能: **はい**（通報機能あり）

- [ ] ✅ コンテンツレーティング完了

### Step 5-5: ターゲットオーディエンス・プライバシー設定

1. Play Console → **ターゲットオーディエンスとコンテンツ**
   - ターゲット年齢: **13歳以上**
2. Play Console → **アプリのコンテンツ** → プライバシーポリシー
   - Phase 4-2 で作成した URL を入力

- [ ] ✅ ターゲット・プライバシー設定完了

---

## Phase 6: GitHub Actions / CI 設定 👤

### Step 6-1: GOOGLE_SERVICES_JSON を Secret に登録

1. GitHub → notHopeless リポジトリ → **Settings** → **Secrets and variables** → **Actions**
2. 「New repository secret」をクリック
3. Name: `GOOGLE_SERVICES_JSON`
4. Value: `android/app/google-services.json` の内容をそのままペースト（JSON全体）
5. 「Add secret」

これにより CI での Android ビルドが本番 Firebase 設定で動作するようになります。

- [ ] ✅ GitHub Secret 設定完了

---

## Phase 7: リリース AAB ビルド & 申請 👤

### Step 7-1: リリース AAB のビルド

（Phase 3 の署名設定完了後）

```bash
cd android
./gradlew bundleRelease
# 出力: android/app/build/outputs/bundle/release/app-release.aab
```

- [ ] ✅ リリース AAB ビルド完了

### Step 7-2: 内部テスト（Internal Testing）

1. Play Console → **内部テスト** → 「新しいリリースを作成」
2. `app-release.aab` をアップロード
3. リリース名（例: `1.0.0-internal-1`）と変更内容を入力
4. テスターのメールアドレスを追加（自分 + 協力者）
5. 実機でインストールして動作確認

確認項目:
- [ ] オンボーディング（3ページスワイプ）が表示される
- [ ] 匿名ログインが成功する
- [ ] ホームフィードが読み込まれる
- [ ] 投稿が送信できる（5分クールダウンを確認）
- [ ] リアクションが動作する（楽観的更新 + ロールバック）
- [ ] 通報が送信できる
- [ ] マイ画面に自分の投稿が表示される
- [ ] 広告枠が表示される（実広告はポリシー審査後に配信される）

- [ ] ✅ 内部テスト完了

### Step 7-3: 本番リリース申請

1. Play Console → **本番** → 「新しいリリースを作成」
2. 同じ `app-release.aab` をアップロード
3. **リリースノート（日本語）**を記入:
   ```
   初回リリース。
   日常の小さな親切を匿名で投稿・共有できるアプリです。
   ```
4. 「変更をレビュー」→「本番への公開を開始」
5. Google のレビュー待ち（通常 1〜3 日、UGC 機能があるため最大 7 日程度かかる場合あり）

- [ ] ✅ 本番リリース申請完了

---

## タスクサマリー（進捗管理用）

| # | タスク | 担当 | 前提 | 状態 |
|---|---|---|---|---|
| 1 | Firebase プロジェクト作成 | 👤 | - | ☐ |
| 2 | 匿名認証 有効化 | 👤 | 1 | ☐ |
| 3 | Firestore 作成 | 👤 | 1 | ☐ |
| 4 | google-services.json 取得・配置 | 👤 | 1 | ☐ |
| 5 | Firebase CLI デプロイ（rules/indexes/functions） | 👤 | 1〜4 | ☐ |
| 6 | stocks 初期データ投入（最低10件） | 👤 | 3 | ☐ |
| 7 | AdMob アカウント + アプリ + 広告ユニット作成 | 👤 | - | ☐ |
| 8 | AdMob ID をコードに反映 | 🤖 | 7 | ☐ |
| 9 | 署名 keystore 作成・保管 | 👤 | - | ☐ |
| 10 | 署名設定 + ProGuard 追加 | 🤖 | 9 | ☐ |
| 11 | プライバシーポリシー 作成・公開 | 👤 | - | ☐ |
| 12 | プライバシーポリシー URL をアプリ内に追加 | 🤖 | 11 | ☐ |
| 13 | GitHub Secret（GOOGLE_SERVICES_JSON）設定 | 👤 | 4 | ☐ |
| 14 | Play Console アカウント作成（$25） | 👤 | - | ☐ |
| 15 | ストア掲載情報 + スクリーンショット | 👤 | 14 | ☐ |
| 16 | コンテンツレーティング | 👤 | 14 | ☐ |
| 17 | リリース AAB ビルド | 👤（🤖サポート） | 10 | ☐ |
| 18 | 内部テスト | 👤 | 17 | ☐ |
| 19 | 本番リリース申請 | 👤 | 18 | ☐ |

**並行して進められる作業**: タスク 1・7・9・11・14 は互いに独立しているため同時に進められます。

---

## 参考リンク

- [Firebase Console](https://console.firebase.google.com/)
- [AdMob Console](https://admob.google.com/)
- [Google Play Console](https://play.google.com/console/)
- [Firebase CLI ドキュメント](https://firebase.google.com/docs/cli)
- [Play ストア掲載要件](https://support.google.com/googleplay/android-developer/answer/9859455)
- [AdMob ポリシー](https://support.google.com/admob/answer/6128543)

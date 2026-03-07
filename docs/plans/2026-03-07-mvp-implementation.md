# notHopeless MVP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Android app「世の中捨てたもんじゃない」MVP をフル実装し GitHub public リポジトリに登録する

**Architecture:** MVVM + Repository パターン。読み取りは Firestore 直接、書き込みは Cloud Functions 経由のハイブリッド構成。匿名認証で登録不要。

**Tech Stack:** Kotlin/Jetpack Compose/Hilt/Navigation/DataStore/Firebase Auth+Firestore+Functions+Analytics+Crashlytics/AdMob / Cloud Functions Node.js 20 (TypeScript)

**Agent Strategy:**
- Task 0–2: メインエージェントが順次実行（GitHub repo + scaffold）
- Task 3–6: バックエンド (Functions) を 1 エージェントで実装
- Task 7–11: Android データ層を 1 エージェントで実装
- Task 12–18: Android UI 層を順次 or 並列で実装
- Task 19: CI + 最終統合

---

## Task 0: GitHub リポジトリ作成 & ベース構造

**Files:**
- Create: `README.md`
- Create: `.gitignore`
- Create: `firebase.json`
- Create: `firestore.rules`
- Create: `firestore.indexes.json`

**Step 1: GitHub public リポジトリ作成**

```bash
gh repo create notHopeless --public --description "世の中捨てたもんじゃない - Android App" --clone
cd notHopeless
```

**Step 2: .gitignore 作成**

```
# Android
*.iml
.gradle/
local.properties
android/.idea/
android/app/build/
android/build/
android/app/google-services.json
android/app/src/debug/google-services.json

# Firebase
.firebase/
firebase-debug.log
firestore-debug.log
ui-debug.log

# Cloud Functions
functions/node_modules/
functions/lib/
functions/.env
functions/.secret.local

# Keys
*.keystore
*.jks
```

**Step 3: firebase.json 作成**

```json
{
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  },
  "functions": [
    {
      "source": "functions",
      "codebase": "default",
      "ignore": ["node_modules", ".git", "firebase-debug.log"]
    }
  ]
}
```

**Step 4: firestore.rules 作成（設計書 §2 の内容）**

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuth() {
      return request.auth != null;
    }

    match /posts/{postId} {
      allow read: if isAuth() && (
        resource.data.status == 'visible' ||
        resource.data.authorId == request.auth.uid
      );
      allow write: if false;
    }

    match /dailyPicks/{date} {
      allow read: if isAuth();
      allow write: if false;
    }

    match /reactions/{id}  { allow read, write: if false; }
    match /reports/{id}    { allow read, write: if false; }
    match /stocks/{id}     { allow read, write: if false; }
    match /users/{uid}     { allow read, write: if false; }
  }
}
```

**Step 5: firestore.indexes.json 作成**

```json
{
  "indexes": [
    {
      "collectionGroup": "posts",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "posts",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "authorId", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "reports",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "postId", "order": "ASCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

**Step 6: docs/ をコピーしてコミット**

```bash
cp -r /path/to/existing/docs ./docs
git add .
git commit -m "chore: initial repo setup with docs and Firestore config"
git push -u origin main
```

---

## Task 1: Cloud Functions プロジェクト初期化

**Files:**
- Create: `functions/package.json`
- Create: `functions/tsconfig.json`
- Create: `functions/src/index.ts`
- Create: `functions/src/validators.ts`
- Create: `functions/src/types.ts`

**Step 1: functions ディレクトリ初期化**

```bash
mkdir functions && cd functions
npm init -y
npm install firebase-admin firebase-functions
npm install --save-dev typescript @types/node ts-jest jest @types/jest
```

**Step 2: tsconfig.json**

```json
{
  "compilerOptions": {
    "module": "commonjs",
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "outDir": "lib",
    "sourceMap": true,
    "strict": true,
    "target": "es2020"
  },
  "compileOnSave": true,
  "include": ["src"]
}
```

**Step 3: package.json scripts 追記**

```json
{
  "scripts": {
    "build": "tsc",
    "build:watch": "tsc --watch",
    "serve": "npm run build && firebase emulators:start --only functions",
    "test": "jest",
    "lint": "tsc --noEmit"
  },
  "jest": {
    "preset": "ts-jest",
    "testEnvironment": "node",
    "testPathPattern": "src/__tests__"
  }
}
```

**Step 4: types.ts 作成（共有型定義）**

```typescript
export const VALID_SCENES = ['commute', 'shop', 'workplace', 'public'] as const;
export const VALID_KINDNESS_TYPES = ['care', 'help', 'integrity', 'courage', 'pro'] as const;
export const VALID_USER_STATES = ['tired', 'rushed', 'down', 'normal'] as const;
export const VALID_EFFECTS = ['relieved', 'lighter', 'inspired', 'survived', 'notHopeless', 'trust'] as const;
export const VALID_REACTION_TYPES = ['notHopeless', 'moved', 'doToo'] as const;
export const VALID_REPORT_REASONS = ['personal_info', 'harassment', 'discrimination', 'sexual', 'other'] as const;

export type Scene = typeof VALID_SCENES[number];
export type KindnessType = typeof VALID_KINDNESS_TYPES[number];
export type UserState = typeof VALID_USER_STATES[number];
export type Effect = typeof VALID_EFFECTS[number];
export type ReactionType = typeof VALID_REACTION_TYPES[number];
export type ReportReason = typeof VALID_REPORT_REASONS[number];
```

**Step 5: validators.ts 作成**

```typescript
// 個人情報・固有名詞パターン（設計書 §3.1 に準拠）
const PERSONAL_INFO_PATTERNS = [
  /\d{2,4}[-\s]\d{2,4}[-\s]\d{4}/,          // 電話番号
  /[^\s]+@[^\s]+\.[^\s]+/,                    // メール
  /https?:\/\//,                               // URL
  /@[A-Za-z0-9_]+/,                           // SNS ID
  /〒\d{3}-\d{4}/,                            // 郵便番号
  /[0-9]+丁目/,                               // 住所
];

const PROPER_NOUN_SUFFIXES = [
  '駅', '店', '会社', '学校', '病院', '公園',
  'さん', 'くん', 'ちゃん', '様',
];

export function normalizeText(text: string): string {
  return text
    .replace(/[\u200B-\u200D\uFEFF]/g, '') // ゼロ幅文字除去
    .replace(/　/g, ' ')                     // 全角スペース→半角
    .trim();
}

export type ValidationResult =
  | { ok: true }
  | { ok: false; code: 'PERSONAL_INFO' | 'SPECIFIC_NOUN' | 'TOO_LONG' | 'MISSING_FIELD' | 'INVALID_ENUM' };

export function validateBody(body: string): ValidationResult {
  if (body.length > 140) return { ok: false, code: 'TOO_LONG' };

  for (const pattern of PERSONAL_INFO_PATTERNS) {
    if (pattern.test(body)) return { ok: false, code: 'PERSONAL_INFO' };
  }

  for (const suffix of PROPER_NOUN_SUFFIXES) {
    const re = new RegExp(`[\\u4E00-\\u9FFF\\u3040-\\u30FF\\uFF00-\\uFFEF]+${suffix}`);
    if (re.test(body)) return { ok: false, code: 'SPECIFIC_NOUN' };
  }

  return { ok: true };
}
```

**Step 6: テスト作成 (src/__tests__/validators.test.ts)**

```typescript
import { normalizeText, validateBody } from '../validators';

describe('normalizeText', () => {
  it('ゼロ幅文字を除去する', () => {
    expect(normalizeText('hello\u200Bworld')).toBe('helloworld');
  });
  it('前後の空白をトリムする', () => {
    expect(normalizeText('  hello  ')).toBe('hello');
  });
});

describe('validateBody', () => {
  it('正常なテキストはOK', () => {
    expect(validateBody('バスで席を譲ってもらいました')).toEqual({ ok: true });
  });
  it('141字超過はTOO_LONG', () => {
    expect(validateBody('あ'.repeat(141))).toEqual({ ok: false, code: 'TOO_LONG' });
  });
  it('メールアドレスはPERSONAL_INFO', () => {
    expect(validateBody('test@example.com に連絡')).toEqual({ ok: false, code: 'PERSONAL_INFO' });
  });
  it('電話番号はPERSONAL_INFO', () => {
    expect(validateBody('090-1234-5678 に電話')).toEqual({ ok: false, code: 'PERSONAL_INFO' });
  });
  it('駅名サフィックスはSPECIFIC_NOUN', () => {
    expect(validateBody('渋谷駅で助けてもらった')).toEqual({ ok: false, code: 'SPECIFIC_NOUN' });
  });
});
```

**Step 7: テスト実行確認**

```bash
cd functions && npm test
# 全テストがPASSすること
```

**Step 8: コミット**

```bash
git add functions/
git commit -m "feat(functions): add Cloud Functions project scaffold with validators"
```

---

## Task 2: Cloud Functions 実装 (createPost / reactToPost / reportPost / buildDailyPicks)

**Files:**
- Create: `functions/src/createPost.ts`
- Create: `functions/src/reactToPost.ts`
- Create: `functions/src/reportPost.ts`
- Create: `functions/src/buildDailyPicks.ts`
- Create: `functions/src/index.ts`
- Create: `functions/src/__tests__/createPost.test.ts`
- Create: `functions/src/__tests__/reactToPost.test.ts`

**Step 1: createPost.ts**

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { normalizeText, validateBody } from './validators';
import {
  VALID_SCENES, VALID_KINDNESS_TYPES, VALID_USER_STATES, VALID_EFFECTS,
  Scene, KindnessType, UserState, Effect,
} from './types';

const COOLDOWN_MS = 5 * 60 * 1000; // 5分

export const createPost = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Login required');
  }
  const uid = context.auth.uid;
  const db = admin.firestore();

  // 入力正規化
  const scene: Scene = data.scene;
  const kindnessType: KindnessType = data.kindnessType;
  const userState: UserState | null = data.userState ?? null;
  const effect: Effect = data.effect;
  const body: string = normalizeText(data.body ?? '');

  // enum バリデーション
  if (!VALID_SCENES.includes(scene)) throw new functions.https.HttpsError('invalid-argument', 'MISSING_FIELD');
  if (!VALID_KINDNESS_TYPES.includes(kindnessType)) throw new functions.https.HttpsError('invalid-argument', 'MISSING_FIELD');
  if (userState !== null && !VALID_USER_STATES.includes(userState)) throw new functions.https.HttpsError('invalid-argument', 'MISSING_FIELD');
  if (!VALID_EFFECTS.includes(effect)) throw new functions.https.HttpsError('invalid-argument', 'MISSING_FIELD');
  if (!body) throw new functions.https.HttpsError('invalid-argument', 'MISSING_FIELD');

  // body バリデーション
  const bodyResult = validateBody(body);
  if (!bodyResult.ok) {
    throw new functions.https.HttpsError('invalid-argument', bodyResult.code);
  }

  // クールダウンチェック
  const userRef = db.doc(`users/${uid}`);
  const userSnap = await userRef.get();
  if (userSnap.exists) {
    const lastPostAt: FirebaseFirestore.Timestamp | undefined = userSnap.data()?.lastPostAt;
    if (lastPostAt && Date.now() - lastPostAt.toMillis() < COOLDOWN_MS) {
      throw new functions.https.HttpsError('resource-exhausted', 'COOLDOWN');
    }
  }

  // 書き込み
  const now = admin.firestore.FieldValue.serverTimestamp();
  const postRef = db.collection('posts').doc();
  const batch = db.batch();
  batch.set(postRef, {
    authorId: uid,
    scene,
    kindnessType,
    userState,
    effect,
    body,
    reactionCounts: { notHopeless: 0, moved: 0, doToo: 0 },
    isStock: false,
    createdAt: now,
    status: 'visible',
  });
  batch.set(userRef, { lastPostAt: now, createdAt: now }, { merge: true });
  await batch.commit();

  return { postId: postRef.id };
});
```

**Step 2: reactToPost.ts**

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { VALID_REACTION_TYPES, ReactionType } from './types';

export const reactToPost = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Login required');
  const uid = context.auth.uid;
  const db = admin.firestore();

  const postId: string = data.postId;
  const type: ReactionType = data.type;

  if (!postId) throw new functions.https.HttpsError('invalid-argument', 'postId required');
  if (!VALID_REACTION_TYPES.includes(type)) throw new functions.https.HttpsError('invalid-argument', 'invalid type');

  const reactionId = `${postId}_${uid}`;
  const reactionRef = db.doc(`reactions/${reactionId}`);
  const postRef = db.doc(`posts/${postId}`);

  await db.runTransaction(async (tx) => {
    const reactionSnap = await tx.get(reactionRef);

    if (!reactionSnap.exists) {
      // 新規
      tx.set(reactionRef, { postId, userId: uid, type, createdAt: admin.firestore.FieldValue.serverTimestamp() });
      tx.update(postRef, { [`reactionCounts.${type}`]: admin.firestore.FieldValue.increment(1) });
    } else {
      const existing = reactionSnap.data()!;
      if (existing.type === type) return; // 冪等
      // 別 type → 上書き
      tx.update(reactionRef, { type });
      tx.update(postRef, {
        [`reactionCounts.${existing.type}`]: admin.firestore.FieldValue.increment(-1),
        [`reactionCounts.${type}`]: admin.firestore.FieldValue.increment(1),
      });
    }
  });

  return {};
});
```

**Step 3: reportPost.ts**

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { VALID_REPORT_REASONS, ReportReason } from './types';

export const reportPost = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Login required');
  const uid = context.auth.uid;
  const db = admin.firestore();

  const postId: string = data.postId;
  const reason: ReportReason = data.reason;

  if (!postId) throw new functions.https.HttpsError('invalid-argument', 'postId required');
  if (!VALID_REPORT_REASONS.includes(reason)) throw new functions.https.HttpsError('invalid-argument', 'invalid reason');

  const postSnap = await db.doc(`posts/${postId}`).get();
  if (!postSnap.exists) throw new functions.https.HttpsError('not-found', 'post not found');

  await db.collection('reports').add({
    postId,
    reporterId: uid,
    reason,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return {};
});
```

**Step 4: buildDailyPicks.ts**

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

// JST = UTC+9
function getTodayJST(): string {
  const now = new Date(Date.now() + 9 * 60 * 60 * 1000);
  return now.toISOString().slice(0, 10);
}

export const buildDailyPicks = functions.pubsub
  .schedule('0 21 * * *') // JST 06:00 = UTC 21:00
  .timeZone('Asia/Tokyo')
  .onRun(async () => {
    const db = admin.firestore();
    const date = getTodayJST();
    const picksRef = db.doc(`dailyPicks/${date}`);

    // 冪等：既に生成済みならスキップ
    if ((await picksRef.get()).exists) return;

    const candidates: string[] = [];
    const seen = new Set<string>();

    // 1. 直近24時間の反応数上位10件
    const since24h = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const topSnap = await db.collection('posts')
      .where('status', '==', 'visible')
      .where('createdAt', '>=', admin.firestore.Timestamp.fromDate(since24h))
      .orderBy('createdAt', 'desc')
      .limit(20)
      .get();

    const sorted = topSnap.docs
      .map(d => ({ id: d.id, total: Object.values(d.data().reactionCounts as Record<string, number>).reduce((a, b) => a + b, 0) }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 10);

    for (const { id } of sorted) {
      if (candidates.length >= 3) break;
      if (!seen.has(id)) { candidates.push(id); seen.add(id); }
    }

    // 2. 直近72時間の新着で補完
    if (candidates.length < 3) {
      const since72h = new Date(Date.now() - 72 * 60 * 60 * 1000);
      const recentSnap = await db.collection('posts')
        .where('status', '==', 'visible')
        .where('createdAt', '>=', admin.firestore.Timestamp.fromDate(since72h))
        .orderBy('createdAt', 'desc')
        .limit(10)
        .get();
      for (const doc of recentSnap.docs) {
        if (candidates.length >= 3) break;
        if (!seen.has(doc.id)) { candidates.push(doc.id); seen.add(doc.id); }
      }
    }

    // 3. stocks から補完
    if (candidates.length < 3) {
      const stocksSnap = await db.collection('stocks')
        .orderBy('usedAt', 'asc')
        .limit(10)
        .get();
      const now = admin.firestore.Timestamp.now();
      const batch = db.batch();
      for (const doc of stocksSnap.docs) {
        if (candidates.length >= 3) break;
        if (!seen.has(doc.id)) {
          // stock を post 相当の形式でピックスに載せる（postId として stock ID を使用）
          candidates.push(doc.id);
          seen.add(doc.id);
          batch.update(doc.ref, { usedAt: now });
        }
      }
      if (candidates.length > 0) await batch.commit();
    }

    await picksRef.set({
      date,
      pickIds: candidates,
      generatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });
```

**Step 5: index.ts**

```typescript
import * as admin from 'firebase-admin';
admin.initializeApp();

export { createPost } from './createPost';
export { reactToPost } from './reactToPost';
export { reportPost } from './reportPost';
export { buildDailyPicks } from './buildDailyPicks';
```

**Step 6: テスト実行**

```bash
cd functions && npm test
```

**Step 7: コミット**

```bash
git add functions/src/
git commit -m "feat(functions): implement createPost, reactToPost, reportPost, buildDailyPicks"
```

---

## Task 3: Android プロジェクト初期化

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts` (root)
- Create: `android/gradle/libs.versions.toml`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/debug/AndroidManifest.xml`
- Create: `android/app/.gitignore`

**Step 1: settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "notHopeless"
include(":app")
```

**Step 2: libs.versions.toml**

```toml
[versions]
agp = "8.4.0"
kotlin = "2.0.0"
ksp = "2.0.0-1.0.21"
hilt = "2.51.1"
compose-bom = "2024.06.00"
navigation-compose = "2.7.7"
hilt-navigation-compose = "1.2.0"
lifecycle = "2.8.2"
datastore = "1.1.1"
coroutines = "1.8.1"
firebase-bom = "33.1.1"
admob = "23.2.0"
coil = "2.6.0"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.0" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-android-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
firebase-functions = { group = "com.google.firebase", name = "firebase-functions-ktx" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }
admob = { group = "com.google.android.gms", name = "play-services-ads", version.ref = "admob" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
junit = { group = "junit", name = "junit", version = "4.13.2" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
mockk = { group = "io.mockk", name = "mockk", version = "1.13.11" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version = "3.0.2" }
```

**Step 3: app/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.nothopeless.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nothopeless.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.admob)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
```

**Step 4: debug/AndroidManifest.xml（cleartext許可）**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:usesCleartextTraffic="true" />
</manifest>
```

**Step 5: main/AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:name=".NotHopelessApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.NotHopeless">
        <!-- AdMob App ID: 本番は google-services.json から自動 -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"/>
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.NotHopeless">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**Step 6: app/.gitignore**

```
google-services.json
```

**Step 7: コミット**

```bash
git add android/
git commit -m "chore(android): initialize Android project scaffold"
```

---

## Task 4: Android データモデル + DataStore

**Files:**
- Create: `android/app/src/main/java/com/nothopeless/app/data/model/Post.kt`
- Create: `android/app/src/main/java/com/nothopeless/app/data/model/DailyPick.kt`
- Create: `android/app/src/main/java/com/nothopeless/app/data/local/DataStoreDataSource.kt`
- Create: `android/app/src/main/java/com/nothopeless/app/NotHopelessApp.kt`

**Step 1: Post.kt（ドメインモデル）**

```kotlin
package com.nothopeless.app.data.model

import com.google.firebase.Timestamp

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val scene: String = "",
    val kindnessType: String = "",
    val userState: String? = null,
    val effect: String = "",
    val body: String = "",
    val reactionCounts: Map<String, Long> = mapOf("notHopeless" to 0, "moved" to 0, "doToo" to 0),
    val isStock: Boolean = false,
    val createdAt: Timestamp? = null,
    val status: String = "visible",
)

enum class SceneType(val key: String, val label: String) {
    COMMUTE("commute", "通勤・通学"),
    SHOP("shop", "お店"),
    WORKPLACE("workplace", "職場"),
    PUBLIC("public", "街・公共"),
}

enum class KindnessType(val key: String, val label: String) {
    CARE("care", "気遣い"),
    HELP("help", "手助け"),
    INTEGRITY("integrity", "誠実"),
    COURAGE("courage", "勇気"),
    PRO("pro", "プロの仕事"),
}

enum class UserStateType(val key: String, val label: String) {
    TIRED("tired", "疲れてた"),
    RUSHED("rushed", "焦ってた"),
    DOWN("down", "落ちてた"),
    NORMAL("normal", "普通"),
}

enum class EffectType(val key: String, val label: String) {
    RELIEVED("relieved", "少し安心した"),
    LIGHTER("lighter", "気持ちが軽くなった"),
    INSPIRED("inspired", "自分も優しくしようと思った"),
    SURVIVED("survived", "今日を乗り切れた"),
    NOT_HOPELESS("notHopeless", "捨てたもんじゃないと思った"),
    TRUST("trust", "人を信じてみようと思った"),
}

enum class ReactionType(val key: String, val label: String) {
    NOT_HOPELESS("notHopeless", "捨てたもんじゃない"),
    MOVED("moved", "沁みた"),
    DO_TOO("doToo", "自分もする"),
}

enum class ReportReason(val key: String, val label: String) {
    PERSONAL_INFO("personal_info", "個人情報・特定できる情報が含まれている"),
    HARASSMENT("harassment", "誹謗中傷・悪口"),
    DISCRIMINATION("discrimination", "差別的な内容"),
    SEXUAL("sexual", "性的な内容"),
    OTHER("other", "その他"),
}
```

**Step 2: DataStoreDataSource.kt**

```kotlin
package com.nothopeless.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class DataStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs -> prefs[ONBOARDING_COMPLETED] = true }
    }
}
```

**Step 3: NotHopelessApp.kt**

```kotlin
package com.nothopeless.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NotHopelessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        MobileAds.initialize(this)
    }
}
```

**Step 4: テスト（DataStore は Integration テストのため Unit はスキップ、モデルのみ確認）**

```kotlin
// android/app/src/test/java/com/nothopeless/app/data/model/PostTest.kt
package com.nothopeless.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PostTest {
    @Test
    fun `SceneType key と label が設計書と一致する`() {
        assertEquals("commute", SceneType.COMMUTE.key)
        assertEquals("通勤・通学", SceneType.COMMUTE.label)
        assertEquals("shop", SceneType.SHOP.key)
    }

    @Test
    fun `ReactionType の全 key が期待通り`() {
        val keys = ReactionType.values().map { it.key }
        assertEquals(listOf("notHopeless", "moved", "doToo"), keys)
    }
}
```

**Step 5: コミット**

```bash
git add android/app/src/
git commit -m "feat(android/data): add data models, enums, and DataStore"
```

---

## Task 5: Android Remote データソース + リポジトリ

**Files:**
- Create: `android/app/src/main/java/com/nothopeless/app/data/remote/FirestoreDataSource.kt`
- Create: `android/app/src/main/java/com/nothopeless/app/data/remote/FunctionsDataSource.kt`
- Create: `android/app/src/main/java/com/nothopeless/app/data/repository/PostRepository.kt`
- Create: `android/app/src/main/java/com/nothopeless/app/data/repository/ReactionRepository.kt`
- Create: `android/app/src/main/java/com/nothopeless/app/data/repository/ReportRepository.kt`
- Create: `android/app/src/main/java/com/nothopeless/app/data/repository/SettingsRepository.kt`

**Step 1: FirestoreDataSource.kt**

```kotlin
package com.nothopeless.app.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nothopeless.app.data.model.Post
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore
) {
    companion object {
        private const val PAGE_SIZE = 20L
    }

    suspend fun getFeed(cursor: Timestamp? = null): List<Post> {
        var query = db.collection("posts")
            .whereEqualTo("status", "visible")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)

        if (cursor != null) {
            query = query.startAfter(cursor)
        }

        return query.get().await().documents.mapNotNull { doc ->
            doc.toObject(Post::class.java)?.copy(postId = doc.id)
        }
    }

    suspend fun getMyPosts(authorId: String): List<Post> {
        return db.collection("posts")
            .whereEqualTo("authorId", authorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(postId = doc.id)
            }
    }

    suspend fun getDailyPickPosts(): List<Post> {
        val today = LocalDate.now(ZoneId.of("Asia/Tokyo")).toString()
        val picksSnap = db.collection("dailyPicks").document(today).get().await()
        val pickIds = picksSnap.get("pickIds") as? List<*> ?: return emptyList()

        if (pickIds.isEmpty()) return emptyList()

        val ids = pickIds.filterIsInstance<String>().take(3)
        // in クエリで一括取得（N+1 禁止）
        return db.collection("posts")
            .whereIn("__name__", ids.map { db.collection("posts").document(it) })
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(postId = doc.id)
            }
    }
}
```

**Step 2: FunctionsDataSource.kt**

```kotlin
package com.nothopeless.app.data.remote

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions
) {
    suspend fun createPost(
        scene: String, kindnessType: String, userState: String?,
        effect: String, body: String
    ): String {
        val data = hashMapOf(
            "scene" to scene, "kindnessType" to kindnessType,
            "userState" to userState, "effect" to effect, "body" to body,
        )
        val result = functions.getHttpsCallable("createPost").call(data).await()
        @Suppress("UNCHECKED_CAST")
        return (result.data as Map<String, Any>)["postId"] as String
    }

    suspend fun reactToPost(postId: String, type: String) {
        val data = hashMapOf("postId" to postId, "type" to type)
        functions.getHttpsCallable("reactToPost").call(data).await()
    }

    suspend fun reportPost(postId: String, reason: String) {
        val data = hashMapOf("postId" to postId, "reason" to reason)
        functions.getHttpsCallable("reportPost").call(data).await()
    }
}
```

**Step 3: PostRepository.kt**

```kotlin
package com.nothopeless.app.data.repository

import com.google.firebase.Timestamp
import com.nothopeless.app.data.model.Post
import com.nothopeless.app.data.remote.FirestoreDataSource
import com.nothopeless.app.data.remote.FunctionsDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirestoreDataSource,
    private val functions: FunctionsDataSource,
) {
    suspend fun getFeed(cursor: Timestamp? = null): List<Post> = firestore.getFeed(cursor)
    suspend fun getDailyPicks(): List<Post> = firestore.getDailyPickPosts()
    suspend fun getMyPosts(authorId: String): List<Post> = firestore.getMyPosts(authorId)
    suspend fun createPost(
        scene: String, kindnessType: String, userState: String?,
        effect: String, body: String
    ): String = functions.createPost(scene, kindnessType, userState, effect, body)
}
```

**Step 4: ReactionRepository.kt + ReportRepository.kt**

```kotlin
// ReactionRepository.kt
package com.nothopeless.app.data.repository

import com.nothopeless.app.data.remote.FunctionsDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReactionRepository @Inject constructor(private val functions: FunctionsDataSource) {
    suspend fun react(postId: String, type: String) = functions.reactToPost(postId, type)
}

// ReportRepository.kt
package com.nothopeless.app.data.repository

import com.nothopeless.app.data.remote.FunctionsDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(private val functions: FunctionsDataSource) {
    suspend fun report(postId: String, reason: String) = functions.reportPost(postId, reason)
}
```

**Step 5: SettingsRepository.kt**

```kotlin
package com.nothopeless.app.data.repository

import com.nothopeless.app.data.local.DataStoreDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStoreDataSource) {
    val onboardingCompleted: Flow<Boolean> = dataStore.onboardingCompleted
    suspend fun completeOnboarding() = dataStore.setOnboardingCompleted()
}
```

**Step 6: コミット**

```bash
git add android/app/src/main/java/com/nothopeless/app/data/
git commit -m "feat(android/data): add Firestore/Functions data sources and repositories"
```

---

## Task 6: Hilt DI モジュール

**Files:**
- Create: `android/app/src/main/java/com/nothopeless/app/di/AppModule.kt`

**Step 1: AppModule.kt**

```kotlin
package com.nothopeless.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance("asia-northeast1")
}
```

**Step 2: コミット**

```bash
git add android/app/src/main/java/com/nothopeless/app/di/
git commit -m "feat(android/di): add Hilt AppModule"
```

---

## Task 7: MainActivity + NavGraph

**Files:**
- Create: `android/app/src/main/java/com/nothopeless/app/MainActivity.kt`
- Create: `android/app/src/main/java/com/nothopeless/app/ui/navigation/NavGraph.kt`

**Step 1: NavGraph.kt**

```kotlin
package com.nothopeless.app.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val POST = "post"
    const val MY = "my"
    const val GUIDELINES = "guidelines"
}
```

**Step 2: MainActivity.kt（匿名認証 + NavGraph）**

```kotlin
package com.nothopeless.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.nothopeless.app.data.repository.SettingsRepository
import com.nothopeless.app.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 匿名認証
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
        setContent {
            var startDestination by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                val completed = settingsRepository.onboardingCompleted.first()
                startDestination = if (completed) "home" else "onboarding"
            }
            startDestination?.let { AppRoot(startDestination = it) }
        }
    }
}
```

**Step 3: コミット**

```bash
git add android/app/src/main/java/com/nothopeless/app/MainActivity.kt \
        android/app/src/main/java/com/nothopeless/app/ui/
git commit -m "feat(android/ui): add MainActivity with anonymous auth and NavGraph skeleton"
```

---

## Task 8: 全画面 UI 実装（Onboarding / Home / Post / My / Guidelines / Report）

> **Note:** 各画面は 1 コミット単位で実装する。画面ごとに ViewModel + Screen + UiState を作る。
> ここでは要点コードのみ示す（フルコードは付録参照）。

**Files 一覧:**
- `ui/onboarding/OnboardingScreen.kt` + `OnboardingViewModel.kt`
- `ui/home/HomeScreen.kt` + `HomeViewModel.kt`
- `ui/home/PostCard.kt`
- `ui/post/PostScreen.kt` + `PostViewModel.kt`
- `ui/my/MyScreen.kt` + `MyViewModel.kt`
- `ui/guidelines/GuidelinesScreen.kt`
- `ui/report/ReportBottomSheet.kt` + `ReportViewModel.kt`
- `ui/common/AdCard.kt`
- `ui/AppRoot.kt`

**UiState 定義（設計書 §4 準拠）**

各 ViewModel の UiState は設計書 §4.1〜§4.6 の data class をそのまま実装する。

**OnboardingViewModel の核心:**

```kotlin
fun nextPage() {
    val current = _uiState.value.currentPage
    if (current < 2) {
        _uiState.update { it.copy(currentPage = current + 1) }
    } else {
        complete()
    }
}

private fun complete() {
    viewModelScope.launch {
        settingsRepository.completeOnboarding()
        _uiState.update { it.copy(isCompleted = true) }
    }
}
```

**HomeViewModel の核心（楽観更新）:**

```kotlin
fun react(postId: String, type: ReactionType) {
    // 即時ローカル更新
    _uiState.update { state ->
        val newFeed = state.feed.map { item ->
            if (item is FeedItem.PostCard && item.post.postId == postId) {
                val updated = item.post.copy(
                    reactionCounts = item.post.reactionCounts.toMutableMap().apply {
                        this[type.key] = (this[type.key] ?: 0) + 1
                    }
                )
                FeedItem.PostCard(updated)
            } else item
        }
        state.copy(feed = newFeed, reactionState = state.reactionState + (postId to type))
    }
    viewModelScope.launch {
        runCatching { reactionRepository.react(postId, type.key) }
            .onFailure { rollbackReaction(postId, type) }
    }
}
```

**フィード構築（AdCard 挿入）:**

```kotlin
private fun buildFeedItems(posts: List<Post>): List<FeedItem> {
    val result = mutableListOf<FeedItem>()
    posts.forEachIndexed { index, post ->
        result.add(FeedItem.PostCard(post))
        // インデックス 10, 21, 32... に AdCard 挿入
        if ((index + 1) % 11 == 10 % 11 + 1 || (index == 10 || index == 21 || index == 32)) {
            // シンプル実装: index が 10,21,32 の後
        }
    }
    // AdCard を index 10, 21, 32 の後ろに挿入（逆順で処理）
    val adIndices = listOf(10, 21, 32, 43).filter { it < posts.size }
    var offset = 0
    for (idx in adIndices) {
        result.add(idx + 1 + offset, FeedItem.AdCard)
        offset++
    }
    return result
}
```

**Step 最終: 全画面コミット**

```bash
git add android/app/src/main/java/com/nothopeless/app/ui/
git commit -m "feat(android/ui): implement all screens (Onboarding, Home, Post, My, Guidelines, Report)"
```

---

## Task 9: CI セットアップ

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `.github/PULL_REQUEST_TEMPLATE.md`

**Step 1: ci.yml**

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  functions-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: functions/package-lock.json
      - run: cd functions && npm ci && npm test

  android-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: temurin
      - uses: gradle/actions/setup-gradle@v3
      - run: chmod +x android/gradlew
      - name: Create google-services.json for CI
        run: |
          if [ -n "${{ secrets.GOOGLE_SERVICES_JSON }}" ]; then
            echo '${{ secrets.GOOGLE_SERVICES_JSON }}' > android/app/google-services.json
          else
            cat > android/app/google-services.json <<'JSON'
          {
            "project_info": {
              "project_number": "000000000000",
              "project_id": "nothopeless-dev-ci",
              "storage_bucket": "nothopeless-dev-ci.appspot.com"
            },
            "client": [{
              "client_info": {
                "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
                "android_client_info": { "package_name": "com.nothopeless.app" }
              },
              "api_key": [{ "current_key": "DUMMY_API_KEY_FOR_CI" }],
              "services": {}
            }],
            "configuration_version": "1"
          }
          JSON
          fi
      - run: cd android && ./gradlew assembleDebug testDebugUnitTest --no-daemon
```

**Step 2: コミット**

```bash
git add .github/
git commit -m "ci: add GitHub Actions for Functions test and Android build"
git push
```

---

## 付録: AgentStrategy（並列実行方針）

| フェーズ | エージェント | タスク |
|---|---|---|
| Phase A | main | Task 0: GitHub repo + Firebase config |
| Phase B | backend | Task 1–2: Cloud Functions 全実装 |
| Phase B | android-data | Task 3–6: Android scaffold + data layer |
| Phase C | android-ui | Task 7–8: 全画面 UI 実装 |
| Phase D | main | Task 9: CI + 最終統合 push |

Phase B は独立しているため並列実行可能。Phase C は Phase B の Android data layer 完了後に開始。

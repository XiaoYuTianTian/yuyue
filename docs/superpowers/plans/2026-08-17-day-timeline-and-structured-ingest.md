# Day Timeline + Structured Ingest Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the day BottomSheet with a full-screen timeline (title-only rows) and replace keyword intent + tool-loop ingest with one MiniMax JSON call that auto-saves records.

**Architecture:** Pure Kotlin parser + done-policy first (unit-tested). Room gains place/people/diaryType. Navigation uses the existing MainActivity overlay stack (Home → DayTimeline → EntryDetail). Ingest becomes `StructuredTurnClient.complete()` with no tools; ViewModel persists `intent=record` entries and shows a 3s title toast.

**Tech Stack:** Kotlin 2.0, Jetpack Compose, Room 2.6, kotlinx.serialization, MiniMax `/v1/chat/completions`, Vosk ASR (unchanged), JUnit 4 JVM tests.

## Global Constraints

- Asia/Shanghai for “today”, `now`, and `done = startAt <= now` at write time.
- MiniMax API key from settings / `config.local.properties`; never print the key.
- One progress indicator while the model runs; never a second analyze pass.
- No ingest preview sheet. No `InputIntents` on the submit path.
- APK delivery: `adb push` to `/sdcard/Download/Yuyue.apk` only — do not `adb install`.
- Debug Room may destructive-migrate (data wipe). Bump `versionName` on each ship.
- Do not implement P2 dedicated recall page, MiniMax TTS, or ONNX embeddings.
- Visual language: existing Navy / Signal / Warn. Do not switch to the 8-16 paper palette.

**Spec:** `docs/superpowers/specs/2026-08-17-day-timeline-and-structured-ingest.md`

---

## File map

| File | Responsibility |
|------|----------------|
| `android-app/src/main/java/com/yuyue/app/llm/StructuredTurn.kt` | JSON DTOs |
| `android-app/src/main/java/com/yuyue/app/llm/StructuredTurnParser.kt` | Strip fences + parse |
| `android-app/src/main/java/com/yuyue/app/llm/DonePolicy.kt` | `done` from timestamps |
| `android-app/src/main/java/com/yuyue/app/llm/StructuredTurnClient.kt` | One-shot MiniMax chat, no tools |
| `android-app/src/test/java/com/yuyue/app/llm/StructuredTurnParserTest.kt` | Parser tests |
| `android-app/src/test/java/com/yuyue/app/llm/DonePolicyTest.kt` | Done tests |
| `android-app/src/main/java/com/yuyue/app/data/local/Entities.kt` | New entry fields |
| `android-app/src/main/java/com/yuyue/app/ui/DayTimelineScreen.kt` | Full-screen day list |
| `android-app/src/main/java/com/yuyue/app/ui/EntryDetailScreen.kt` | Read-first detail |
| `android-app/src/main/java/com/yuyue/app/ui/components/TitleSavedToast.kt` | 3s title popup |
| `android-app/src/main/java/com/yuyue/app/ui/HomeScreen.kt` | Remove DayDetailSheet; result card; clarify |
| `android-app/src/main/java/com/yuyue/app/MainActivity.kt` | Overlay: Day / Entry |
| `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt` | `submitStructuredTurn` |
| `android-app/src/main/java/com/yuyue/app/data/YuyueRepository.kt` | Persist structured entries; FTS recall |

---

### Task 1: JVM tests + parser + done policy

**Files:**
- Create: `android-app/src/main/java/com/yuyue/app/llm/StructuredTurn.kt`
- Create: `android-app/src/main/java/com/yuyue/app/llm/StructuredTurnParser.kt`
- Create: `android-app/src/main/java/com/yuyue/app/llm/DonePolicy.kt`
- Create: `android-app/src/test/java/com/yuyue/app/llm/StructuredTurnParserTest.kt`
- Create: `android-app/src/test/java/com/yuyue/app/llm/DonePolicyTest.kt`
- Modify: `android-app/build.gradle.kts` (add `testImplementation` JUnit + serialization)
- Modify: `gradle/libs.versions.toml` if adding `junit`

**Interfaces:**
- Produces: `StructuredTurnParser.parse(raw: String): StructuredTurn`
- Produces: `DonePolicy.isDone(startAtMillis: Long, nowMillis: Long): Boolean`

- [ ] **Step 1: Add test deps**

In `android-app/build.gradle.kts` `dependencies`:

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation(libs.kotlinx.serialization.json)
testImplementation(libs.kotlinx.coroutines.core)
```

- [ ] **Step 2: Write failing parser + done tests**

`DonePolicyTest.kt`:

```kotlin
package com.yuyue.app.llm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DonePolicyTest {
    @Test fun pastIsDone() {
        assertTrue(DonePolicy.isDone(1_000L, 2_000L))
    }
    @Test fun nowIsDone() {
        assertTrue(DonePolicy.isDone(2_000L, 2_000L))
    }
    @Test fun futureIsPending() {
        assertFalse(DonePolicy.isDone(3_000L, 2_000L))
    }
}
```

Parser tests must cover: bare JSON; fenced ` ```json `; `record` with two entries; `clarify` with question; invalid JSON throws.

- [ ] **Step 3: Run tests, expect compile/fail**

Run: `.\gradlew.bat :android-app:testDebugUnitTest --tests com.yuyue.app.llm.DonePolicyTest --no-daemon`

Expected: FAIL (class missing) or compilation error.

- [ ] **Step 4: Implement DTOs, parser, DonePolicy**

`StructuredTurn` fields match the spec JSON (`intent`, `needMore`, `clarifyQuestion`, `entries`, `recallQuery`) with `@SerialName` snake_case.

Parser: take substring from first `{` to last `}`; `Json { ignoreUnknownKeys = true; isLenient = true }`.

`DonePolicy.isDone` = `startAtMillis <= nowMillis`.

- [ ] **Step 5: Re-run unit tests, expect PASS**

Run: `.\gradlew.bat :android-app:testDebugUnitTest --tests "com.yuyue.app.llm.*" --no-daemon`

Expected: BUILD SUCCESSFUL, tests PASS.

---

### Task 2: Room fields for place / people / diaryType

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/data/local/Entities.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/data/local/YuyueDatabase.kt` (version 3 → 4)
- Modify: `android-app/src/main/java/com/yuyue/app/data/local/Daos.kt` (FTS entity columns if present)

**Interfaces:**
- Produces: `EntryEntity.place: String?`, `people: String?`, `diaryType: String?`

- [ ] **Step 1: Add the three fields** with defaults `null` on `EntryEntity`.

- [ ] **Step 2: Set `@Database version = 4`**. Keep `.fallbackToDestructiveMigration()`.

- [ ] **Step 3: If `EntryFtsEntity` exists, add the same text fields** and update insert sites in `LocalVectorStore.indexText`.

- [ ] **Step 4: Compile**

Run: `.\gradlew.bat :android-app:compileDebugKotlin --no-daemon`

Expected: SUCCESS. Fix every call site that constructs `EntryEntity` (add the new params or rely on defaults).

---

### Task 3: Day timeline full screen (P0 UI)

**Files:**
- Create: `android-app/src/main/java/com/yuyue/app/ui/DayTimelineScreen.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/MainActivity.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/ui/HomeScreen.kt` (remove `DayDetailSheet`)
- Modify: `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt` (`openDay` still loads entries)

**Interfaces:**
- Consumes: `vm.dayEntries`, `vm.selectedDay` or a `LocalDate` passed in
- Produces: `onEntryClick(EntryEntity)`, `onBack()`

- [ ] **Step 1: Add `DayTimelineScreen`**

Layout: top bar (date + weekday + count + back); `LazyColumn` of rows: `HH:mm` | title | 4dp Signal/Warn dot. Empty copy: `这一天还没有记录`.

Sort: `entries.sortedBy { it.startAtMillis }`.

Enter animation: `AnimatedVisibility` or `Modifier.graphicsLayer` + `Animatable` per index delay `min(index, 30) * 45` ms.

`@Preview` with 0 / 3 fake entries.

- [ ] **Step 2: Wire navigation in `MainActivity`**

Replace overlay enum usage with something like:

```kotlin
var dayOpen by remember { mutableStateOf<java.time.LocalDate?>(null) }
```

When `dayOpen != null` show `DayTimelineScreen` instead of Home (or on top of a back stack). Home `onDayClick` → `dayOpen = date` (do **not** show `DayDetailSheet`).

Keep the voice orb **on the day screen** as well (pass `transcriber` + same submit). Simplest: extract the bottom bar from Home into a shared composable used by Home and Day.

- [ ] **Step 3: Delete the call to `DayDetailSheet` from HomeScreen.** Leave `DaySheet.kt` file until Task 8.

- [ ] **Step 4: Manual check via Preview**; then debug assemble if the rest of the app still compiles.

---

### Task 4: Entry detail page (title → full note)

**Files:**
- Create: `android-app/src/main/java/com/yuyue/app/ui/EntryDetailScreen.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/MainActivity.kt`
- Optionally reuse save path from `EntryEditScreen` (`vm.patchEntry` / `setEntryDone`)

**Interfaces:**
- Consumes: `EntryEntity`
- Produces: back; toggle done; optional edit body later (P1 can keep edit via existing `EntryEditScreen` overflow)

- [ ] **Step 1: Read-first detail**: large title, done color edge, body, collapsible raw transcript, place/people/type as one muted line, Switch for done.

- [ ] **Step 2: From timeline row click → detail overlay.** Back returns to the same day.

- [ ] **Step 3: `@Preview` a green past note and a red future note.**

---

### Task 5: Structured MiniMax client (no tools)

**Files:**
- Create: `android-app/src/main/java/com/yuyue/app/llm/StructuredTurnClient.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/agent/MiniMaxChatClient.kt` **or** add `chatTextOnly(messages)` that omits `tools`

**Interfaces:**
- Produces: `suspend fun complete(nowIso: String, userText: String, history: List<ChatMessage>, notebookId: String, notebookName: String, placeHint: String?): StructuredTurn`

- [ ] **Step 1: Add a no-tools completion method**

POST same `/v1/chat/completions` body **without** `tools`. System prompt (exact behavior):

- Output **only** the JSON object from the spec.
- `intent` one of record/recall/clarify.
- Missing time → use `nowIso`.
- Missing place → empty (client may fill).
- Missing people → `[]`.
- Titles ≤ 20 Chinese characters.
- Split multiple events into multiple `entries`.
- If the user did not say enough to record or search, `intent=clarify` and a short `clarify_question`.

- [ ] **Step 2: On parse failure, retry once** with an extra user message: `只输出 JSON，不要其它文字。` If still failing, throw a typed `StructuredTurnParseException`.

- [ ] **Step 3: Unit-test the prompt is not required; parser already tested.** Compile the client.

---

### Task 6: Persist record + 3s title toast + submit path

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/data/YuyueRepository.kt` (`saveStructuredEntries`)
- Modify: `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/ui/HomeScreen.kt` (+ Day screen if it shares submit)
- Create: `android-app/src/main/java/com/yuyue/app/ui/components/TitleSavedToast.kt`

**Interfaces:**
- Consumes: `StructuredTurnClient.complete`
- Produces: `submitStructuredTurn(text: String)` replacing `submitIngest` on the voice button

- [ ] **Step 1: `saveStructuredEntries`**

For each entry: parse `start_at` with `TimeUtils.parseIso`; `done = DonePolicy.isDone(...)`; upsert Room; FTS `indexText` immediately; embedding `indexEntry` **after** `_jobRunning = false` (same pattern as current ingest).

Shared `utteranceId`.

- [ ] **Step 2: ViewModel routing**

```
clarify → _clarifyQuestion + keep draft (message / state)
record  → save → _savedTitle (first title or “已记下 n 条”) → refresh month/day
recall  → repository.filterByRecallQuery → _recallHits for result card
parse/http fail → message, keep draft
```

Guard `_jobRunning` so double-tap cannot start a second call.

- [ ] **Step 3: `TitleSavedToast`**

Full-width-ish surface, title text, Signal accent. `LaunchedEffect` delay 3000ms then dismiss. One LinearProgress in the draft card only while `jobRunning`.

- [ ] **Step 4: Voice button label stays「分析」or「记录」** — use **「记录」** as the user asked. `submitDraft` always calls `submitStructuredTurn`. Delete classify/when Ingest/Recall/Mutate from this path. Recall intent no longer navigates to `RecallScreen`.

- [ ] **Step 5: Clarify UX**

Draft card title area shows `clarify_question`. User can keep holding-to-talk to append. History: previous user text + assistant JSON or question, then new user text.

---

### Task 7: P1 recall result card (not a new app section)

**Files:**
- Create or modify a small `RecallResultCard` in `ui/components/`
- Modify: `YuyueRepository` with `fun filterRecall(q: RecallQuery): List<EntryEntity>` using `time_from/to`, `diary_type`, FTS `keyword`/`title_hint`

**Interfaces:**
- Produces: list of titles; tap opens `EntryDetailScreen`

- [ ] **Step 1: Filter implementation** — if all query fields null, return empty (do not dump the whole notebook).

- [ ] **Step 2: Card UX** — one headline “找到 n 则”, list of `HH:mm · title`, empty “没有找到相关日记”. Appears above the orb, same layer as the old draft.

- [ ] **Step 3: No second LLM call in P1** for rewriting the answer. (P2 will add a polish pass.)

---

### Task 8: Dead-code cleanup + version ship

**Files:**
- Stop using: `InputIntent.kt` on submit (may keep file)
- Stop showing: `IngestPreviewSheet`
- Optional delete later: unused confirm ingest VM methods if nothing references them
- Modify: `android-app/build.gradle.kts` `versionCode` +1, `versionName` `1.0.7+` timestamp

- [ ] **Step 1: Remove preview + keyword routing from Home.**

- [ ] **Step 2: `assembleDebug`**

Run from repo root:

```
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
.\gradlew.bat :android-app:assembleDebug --no-daemon
.\gradlew.bat :android-app:testDebugUnitTest --no-daemon
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Push APK**

```
C:\Android\Sdk\platform-tools\adb.exe push android-app\build\outputs\apk\debug\android-app-debug.apk /sdcard/Download/Yuyue.apk
```

Tell the user to sideload and confirm `versionName` on the home subtitle.

- [ ] **Step 4: Device UX checklist** from spec §9.

---

## Spec coverage

| Spec section | Task |
|--------------|------|
| Day full screen + title rows | 3 |
| Detail page | 4 |
| JSON schema + parse | 1, 5 |
| Auto record + 3s toast | 6 |
| done hard rule | 1, 6 |
| clarify loop | 6 |
| recall card P1 | 7 |
| Room extra fields | 2 |
| Kill preview / keyword router | 6, 8 |
| Vosk unchanged / TTS later | — (out of scope) |

## Placeholder scan

None intended. P2/P3 called out as out of scope, not as TODOs inside tasks.

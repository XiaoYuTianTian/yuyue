# Async Capture Outbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After the user sends a utterance, the UI unblocks immediately; organizing runs in the background as a ledger task; the user can pause/edit/discard from the bottom bar; completion toasts can jump to the resulting diary.

**Architecture:** Keep `CaptureLedger` as the only outbox (no staging Entry). Add `CaptureStatus.Paused`. `submitStructuredTurn` saves the raw line, releases `jobRunning`, and continues the existing StructuredTurn path on a cancelable per-request `Job`. Do **not** enqueue empty-history `CaptureQueueWork` while that job is alive. WorkManager remains for retry/offline only. Toast actions become typed (`Undo` vs `OpenEntry`) so “查看” does not call `undoLast`.

**Tech Stack:** Kotlin, Jetpack Compose, Room, WorkManager, JUnit 4, `assembleDebug` + `adb push` to `/sdcard/Download/Yuyue.apk`.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-22-async-capture-outbox-design.md`
- **Never** insert a guessed-date draft into the home/day feed before the model (or deterministic mutate) confirms.
- Do not add a top-of-list “整理中” banner (scheme **A**: bottom bar + detail only).
- Editing raw text is allowed only when status is `Paused` (or after user pauses a `ForegroundRequired` item).
- Do not overwrite user-edited Settings slogans.
- APK delivery: `adb push` to `/sdcard/Download/Yuyue.apk` — do not rely on `adb install`.
- This workspace may not be a git repo: **skip Commit steps** if `git status` fails.

## File map

| File | Responsibility |
|------|----------------|
| `android-app/src/main/java/com/yuyue/app/data/capture/CaptureLedgerEngine.kt` | Add `Paused` to `CaptureStatus`; engine skips paused rows |
| `android-app/src/test/java/com/yuyue/app/capture/CaptureLedgerEngineTest.kt` | Engine tests for skip-paused |
| `android-app/src/main/java/com/yuyue/app/data/capture/CaptureQueueModels.kt` | Labels + `CaptureCompletedNotice` fields + `ToastKind` |
| `android-app/src/main/java/com/yuyue/app/data/capture/CaptureLedgerService.kt` | `markPaused`, `updateRawTranscript`, observe includes Paused |
| `android-app/src/main/java/com/yuyue/app/data/capture/CaptureQueueWork.kt` | `cancel(context, requestId)`; skip paused in worker |
| `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt` | Unblock submit; cancelable jobs; pause/resume/edit; jump toast |
| `android-app/src/main/java/com/yuyue/app/ui/components/YuyueToastHost.kt` | ActionPerformed → undo **or** open entry |
| `android-app/src/main/java/com/yuyue/app/ui/components/CaptureQueueBar.kt` | Pause / continue / edit raw when Paused |
| `android-app/src/main/java/com/yuyue/app/MainActivity.kt` | Collect `openEntryRequest` → `viewingEntry` |

---

### Task 1: `Paused` status + engine skip + notice payload

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/data/capture/CaptureLedgerEngine.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/data/capture/CaptureQueueModels.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/data/capture/CaptureLedgerService.kt`
- Modify: `android-app/src/test/java/com/yuyue/app/capture/CaptureLedgerEngineTest.kt` (add cases; read existing style first)

**Interfaces:**
- Produces:
  - `CaptureStatus.Paused`
  - `CaptureCompletedNotice(requestId, preview, entryId: String? = null)`
  - `CaptureLedgerService.markPaused(requestId)`
  - `CaptureLedgerService.updateRawTranscript(requestId, raw: String)`
  - `observeActiveQueue` includes `Paused`
  - `pendingRequestIds` **excludes** `Paused` (do not auto-retry paused)

- [ ] **Step 1: Add enum + skip in engine**

In `CaptureStatus`:

```kotlin
enum class CaptureStatus {
    Pending,
    Processing,
    Retryable,
    ForegroundRequired,
    Paused,
    Completed,
}
```

Near the top of `CaptureLedgerEngine.process`, after loading `existing`:

```kotlin
if (existing.status == CaptureStatus.Paused) return ProcessResult.Skipped
if (existing.status == CaptureStatus.Completed) return ProcessResult.Completed
```

(Keep the existing Completed short-circuit.)

- [ ] **Step 2: Service APIs + queue observation**

```kotlin
suspend fun markPaused(requestId: String) {
    val row = dao.getByRequestId(requestId) ?: return
    if (row.status == CaptureStatus.Completed.name) return
    dao.upsert(row.copy(status = CaptureStatus.Paused.name, lastError = "已暂停"))
}

suspend fun updateRawTranscript(requestId: String, raw: String) {
    val row = dao.getByRequestId(requestId) ?: return
    dao.upsert(row.copy(rawTranscript = raw.trim()))
}
```

`observeActiveQueue` status list: add `CaptureStatus.Paused.name`.

`pendingRequestIds`: keep only Pending / Retryable / Processing (no Paused).

`CaptureCompletedNotice`:

```kotlin
data class CaptureCompletedNotice(
    val requestId: String,
    val preview: String,
    val entryId: String? = null,
)
```

`userLabel` add:

```kotlin
CaptureStatus.Paused -> "已暂停"
```

- [ ] **Step 3: Unit test — paused is skipped**

Add a test that upserts a Paused record, calls `process`, expects `ProcessResult.Skipped` and unchanged status. Follow helpers already in `CaptureLedgerEngineTest.kt`.

- [ ] **Step 4: Run tests**

```powershell
.\gradlew.bat :android-app:testDebugUnitTest --tests com.yuyue.app.capture.CaptureLedgerEngineTest
```

Expected: BUILD SUCCESSFUL, tests PASS.

- [ ] **Step 5: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/data/capture android-app/src/test/java/com/yuyue/app/capture/CaptureLedgerEngineTest.kt
git commit -m "feat: add paused capture status and skip it in the engine"
```

---

### Task 2: WorkManager cancel + worker honors pause

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/data/capture/CaptureQueueWork.kt`

**Interfaces:**
- Produces: `CaptureQueueWork.cancel(context, requestId)` unique name `capture-ledger-$requestId`

- [ ] **Step 1: Add cancel**

```kotlin
fun cancel(context: Context, requestId: String) {
    WorkManager.getInstance(context).cancelUniqueWork("$WORK_NAME_PREFIX$requestId")
}
```

- [ ] **Step 2: Worker skip paused**

`processCaptureInBackground` already goes through the engine; Paused → Skipped → `Result.success()`. No extra worker branch required if Task 1 is done. Still call `cancel` from ViewModel before `markPaused`.

- [ ] **Step 3: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/data/capture/CaptureQueueWork.kt
git commit -m "feat: cancel per-request capture work on pause"
```

---

### Task 3: Typed toasts + open-entry request

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/ui/components/YuyueToastHost.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/MainActivity.kt`

**Interfaces:**
- Produces:
  - `enum class ToastKind { None, Undo, OpenEntry }` (can live in `AppViewModel.kt` or `CaptureQueueModels.kt`)
  - `val toastKind: StateFlow<ToastKind>`
  - `val pendingOpenEntryId: StateFlow<String?>`
  - `fun consumeOpenEntry(): EntryEntity?` or `SharedFlow<String>` `openEntryRequests`
  - `ObserveYuyueToasts` calls `vm.onToastAction()` instead of always `undoLast()`

Recommended:

```kotlin
private val _toastKind = MutableStateFlow(ToastKind.None)
val toastKind: StateFlow<ToastKind> = _toastKind
private val _openEntryRequests = MutableSharedFlow<String>(extraBufferCapacity = 8)
val openEntryRequests: SharedFlow<String> = _openEntryRequests.asSharedFlow()
private var pendingOpenEntryId: String? = null

private fun emitToast(text: String, actionLabel: String? = null, kind: ToastKind = ToastKind.None, openEntryId: String? = null) {
    pendingOpenEntryId = openEntryId
    _toastKind.value = kind
    _toastAction.value = actionLabel
    _message.value = text
}

fun onToastAction() {
    when (_toastKind.value) {
        ToastKind.Undo -> undoLast()
        ToastKind.OpenEntry -> pendingOpenEntryId?.let { _openEntryRequests.tryEmit(it) }
        ToastKind.None -> Unit
    }
}
```

`clearMessage` also resets `_toastKind` and `pendingOpenEntryId`.

Existing `emitToast(..., "撤销")` must pass `kind = ToastKind.Undo`.

- [ ] **Step 1: Change `ObserveYuyueToasts`**

```kotlin
if (result == SnackbarResult.ActionPerformed) vm.onToastAction()
```

- [ ] **Step 2: MainActivity collect**

Inside `YuyueTheme` / after `vm` exists:

```kotlin
LaunchedEffect(Unit) {
    vm.openEntryRequests.collect { id ->
        val e = vm.entryForOpen(id) // add: repository.getEntry via a small VM helper
        if (e != null) viewingEntry = e
    }
}
```

Add:

```kotlin
fun entryForOpen(id: String): EntryEntity? = /* cannot be sync if repo is suspend */
```

Use:

```kotlin
fun requestOpenEntry(id: String) {
    viewModelScope.launch {
        val e = repository.getEntry(id) ?: return@launch
        _openEntryReady.emit(e)  // SharedFlow<EntryEntity>
    }
}
```

Then toast OpenEntry → `requestOpenEntry(id)`; MainActivity collects `openEntryReady`.

- [ ] **Step 3: Completion toast with 查看**

In `CaptureEvents.completed` collector:

```kotlin
emitToast(
    "已记下：${notice.preview}",
    actionLabel = if (notice.entryId != null) "查看" else null,
    kind = if (notice.entryId != null) ToastKind.OpenEntry else ToastKind.None,
    openEntryId = notice.entryId,
)
```

Update `CaptureLedgerService.processCaptureInBackground` emit to pass first saved id if available; if background path only has preview, leave `entryId` null until Task 4 wires persist ids.

For foreground Record success in ViewModel (Task 4), emit the same toast with first persisted id.

- [ ] **Step 4: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt android-app/src/main/java/com/yuyue/app/ui/components/YuyueToastHost.kt android-app/src/main/java/com/yuyue/app/MainActivity.kt
git commit -m "feat: toast 查看 opens the saved diary"
```

---

### Task 4: Unblock submit — cancelable background organize

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt`

**Interfaces:**
- Produces:
  - `private val organizeJobs = mutableMapOf<String, Job>()`
  - After `saveCapture` succeeds: enqueue is **not** immediate; `_jobRunning = false`; `_voiceNonce += 1`; toast `原话已保存` (no 稍等片刻 blocking copy)
  - Then `organizeJobs[requestId] = viewModelScope.launch { … existing turn logic … }`
  - On failure: `markRetryable` + `CaptureQueueWork.enqueue` (same as today)
  - On success Record: `markCompleted`; emit completed notice with `entryId`
  - **Do not** call `CaptureQueueWork.enqueue` at the start of an interactive turn (already removed once; keep it that way)

Critical: `_jobRunning` must become `false` **before** `structuredTurn.complete` so VoiceDock / calendar stay usable. Do **not** set `_jobRunning = true` for the whole organize.

Clarification UI (`_clarifyQuestion`) may still appear when the job finishes — that is OK; user is not frozen during the wait.

`submitStructuredTurn` early-return if text blank / no notebook. Remove the gate `if (_jobRunning.value) return false` **or** replace it with “allow concurrent organizes” (spec: user can send another line). Allow concurrent: do **not** block on `_jobRunning`.

Keep `_jobRunning` only for mutate-from-pick / commit draft if those still need a spinner; or drop it from VoiceDock `analyzing` for organize. VoiceDock uses `jobRunning` to show AnalyzingBlock — after this task, sending must **not** show AnalyzingBlock. Pass `jobRunning = false` for organize, or stop setting it in `submitStructuredTurn`.

- [ ] **Step 1: Refactor submit**

Structure:

```kotlin
fun submitStructuredTurn(text: String): Boolean {
    val nbId = _selectedNotebookId.value ?: run { emitToast("还没有记事本，请先添加一个"); return false }
    if (text.isBlank()) { emitToast("还没有可发送的文字"); return false }
    viewModelScope.launch {
        val requestId = runCatching { captureLedger.saveCapture(text, nbId, nbName) }
            .getOrElse { emitToast("原话保存失败，请重试"); return@launch }
        _voiceNonce.value += 1
        emitToast("原话已保存")
        val job = launch { runOrganize(requestId, text, nbId, nbName) }
        organizeJobs[requestId] = job
        job.invokeOnCompletion { organizeJobs.remove(requestId) }
    }
    return true
}
```

Move the current `runCatching { structuredTurn.complete … }` body into `runOrganize`.

If `runOrganize` is cancelled (`CancellationException`), `markPaused` if still processing (pause path also cancels the job).

- [ ] **Step 2: persistRecorded → notice**

After successful record persist, emit:

```kotlin
CaptureEvents.emitCompleted(
    CaptureCompletedNotice(requestId, titlePreview, entryId = ids.firstOrNull()),
)
```

Avoid double toast: either stop the generic collector toast **or** only emit from one place. Prefer **only** `CaptureEvents` + collector (remove inline “已记下” duplicates if any).

- [ ] **Step 3: Manual / emulator check**

Send a line, immediately scroll the home list and open settings while the model is still thinking. Feed must not gain a new row until complete.

- [ ] **Step 4: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt
git commit -m "feat: organize utterances in background without locking the dock"
```

---

### Task 5: Pause / resume / edit raw in ViewModel + queue UI

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/ui/components/CaptureQueueBar.kt`
- Wire existing `onDiscard` / `onRetry` / `onContinue` (already passed from Welcome/Day/Calendar)

**Interfaces:**
- Produces:
  - `fun pauseCapture(requestId: String)`
  - `fun resumeCapture(requestId: String)` — `update` if draft text provided, then `markRetryable`/`Pending` + start `runOrganize` **or** `CaptureQueueWork.enqueue`
  - `fun updateCaptureTranscript(requestId: String, raw: String)`

`pauseCapture`:

```kotlin
fun pauseCapture(requestId: String) {
    organizeJobs.remove(requestId)?.cancel()
    CaptureQueueWork.cancel(app, requestId)
    viewModelScope.launch {
        captureLedger.markPaused(requestId)
        if (pendingCaptureRequestId == requestId) { /* keep clarify UI or clear? keep */ }
        emitToast("已暂停")
    }
}
```

`resumeCapture(requestId, rawOverride: String? = null)`:

```kotlin
viewModelScope.launch {
    if (!rawOverride.isNullOrBlank()) captureLedger.updateRawTranscript(requestId, rawOverride)
    val raw = captureLedger.getRawTranscript(requestId) ?: return@launch
    captureLedger.markRetryable(requestId, "继续整理")
    val nbId = _selectedNotebookId.value ?: return@launch
    val nbName = ...
    val job = launch { runOrganize(requestId, raw, nbId, nbName) }
    organizeJobs[requestId] = job
}
```

- [ ] **Step 1: CaptureQueueBar actions**

Extend signature:

```kotlin
fun CaptureQueueBar(
    items: List<CaptureQueueItem>,
    onDiscard: (String) -> Unit = {},
    onRetry: (String) -> Unit = {},
    onContinue: (String) -> Unit = {},
    onPause: (String) -> Unit = {},
    onResume: (String, String?) -> Unit = { _, _ -> },
)
```

In the detail dialog, per item:

- `Processing` / `Pending` / `Retryable`: **暂停**, **丢弃**; Retryable also **重试**
- `Paused`: `OutlinedTextField` bound to local `var draft`; **保存并继续** → `onResume(id, draft)`; **丢弃**
- `ForegroundRequired`: **继续** (existing), **暂停**, **丢弃**

Wire Welcome / Day / Calendar / search-mode bar with `onPause` / `onResume`.

- [ ] **Step 2: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt android-app/src/main/java/com/yuyue/app/ui/components/CaptureQueueBar.kt android-app/src/main/java/com/yuyue/app/ui/WelcomeScreen.kt android-app/src/main/java/com/yuyue/app/ui/DayTimelineScreen.kt android-app/src/main/java/com/yuyue/app/ui/CalendarScreen.kt
git commit -m "feat: pause, edit, and resume capture tasks from the queue"
```

---

### Task 6: Build, push, acceptance

**Files:** none new

- [ ] **Step 1: Tests + APK**

```powershell
.\gradlew.bat :android-app:testDebugUnitTest --tests com.yuyue.app.capture.CaptureLedgerEngineTest :android-app:assembleDebug
C:\Android\Sdk\platform-tools\adb.exe push android-app\build\outputs\apk\debug\android-app-debug.apk /sdcard/Download/Yuyue.apk
```

- [ ] **Step 2: Manual checklist (from spec)**

- Send utterance → within 1s can scroll home / open settings; bottom bar shows the task
- While model runs 10s+, feed has **no** new placeholder row
- On success, toast **查看** opens that entry
- Pause → edit raw → continue uses new text
- Discard → gone from queue and never in feed
- Rate-limit item can retry; raw kept
- Search mode still shows the queue bar

- [ ] **Step 3: Commit** if anything left (skip if no git)

---

## Spec coverage

| Spec | Task |
|------|------|
| Unblock UI after send | 4 |
| Status on bottom bar + detail only | 5 (existing bar) |
| Pause / resume / discard / edit when paused | 1, 2, 5 |
| Completion toast jump | 3, 4 |
| No guessed feed row | 4 (no Entry write until persistRecorded) |
| WorkManager does not steal paused jobs | 1, 2 |
| Search keeps bar | already shipped; keep wiring in 5 |

## Type consistency

- Status name: `Paused` everywhere (enum, Room string, UI label `已暂停`)
- Notice: `CaptureCompletedNotice.entryId`
- Toast: `ToastKind.Undo` / `ToastKind.OpenEntry`
- Jobs: `organizeJobs[requestId]`

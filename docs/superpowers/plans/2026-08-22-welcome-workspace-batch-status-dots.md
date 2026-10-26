# Welcome · Workspace Batch · Status Dots Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a done/pending status dot on every diary row, add a one-time teaching first-launch screen, and let settings edit a workspace then batch-soft-delete its diaries.

**Architecture:** Status dots are a pure rule change (`MonthDots` + remove `MemoryKind.isTaskOrEvent` gate on feed rows). First launch is a gate in `MainActivity` driven by `ProfileStore.welcomeSeen`. Workspace batch adds a detail overlay + multi-select list that calls a new repository soft-delete-by-ids API (same trash path as single delete).

**Tech Stack:** Kotlin, Jetpack Compose, Room, EncryptedSharedPreferences (`ProfileStore`), JUnit 4 JVM tests, Gradle `:android-app:assembleDebug`, `adb push` to `/sdcard/Download/Yuyue.apk`.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-22-welcome-workspace-batch-status-dots-design.md`
- Do **not** overwrite user hand-edited Settings copy for 个人资料 / 备份日记 / 恢复日记 / 关于语曰 / 模型设置 title.
- Soft-delete only for batch delete (废纸篓); never hard-delete.
- Keep ≥1 notebook when deleting a workspace (existing rule).
- First-launch voice button is **illustrative only** (toast + enter home); no ASR.
- APK delivery: `adb push` to `/sdcard/Download/Yuyue.apk` after shippable changes — do not rely on `adb install`.
- This workspace may not be a git repo: **skip Commit steps** if `git status` fails; do not init git unless the user asks.
- Asia/Shanghai unchanged for day labels.

## File map

| File | Responsibility |
|------|----------------|
| `android-app/src/main/java/com/yuyue/app/util/MonthDots.kt` | Calendar day color: any undone → Red |
| `android-app/src/test/java/com/yuyue/app/util/MonthDotsTest.kt` | JVM tests for new rule |
| `android-app/src/main/java/com/yuyue/app/ui/WelcomeScreen.kt` | Always show status dot on `WelcomeFeedRow` |
| `android-app/src/main/java/com/yuyue/app/data/ProfileStore.kt` | `isWelcomeSeen` / `markWelcomeSeen` |
| `android-app/src/main/java/com/yuyue/app/ui/FirstLaunchScreen.kt` | Teaching UI + 开始使用 / mic hint |
| `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt` | welcomeSeen state; notebook entries; batch delete |
| `android-app/src/main/java/com/yuyue/app/MainActivity.kt` | Gate first launch; workspace overlays |
| `android-app/src/main/java/com/yuyue/app/data/local/Daos.kt` | observe + softDelete by ids |
| `android-app/src/main/java/com/yuyue/app/data/YuyueRepository.kt` | `observeActiveEntries` / `deleteEntries` |
| `android-app/src/main/java/com/yuyue/app/ui/WorkspaceDetailScreen.kt` | Edit name/color + link to manage |
| `android-app/src/main/java/com/yuyue/app/ui/WorkspaceEntriesScreen.kt` | Multi-select + batch trash |
| `android-app/src/main/java/com/yuyue/app/ui/SettingsScreen.kt` | Route「编辑」to detail screen callback |

**Name note:** Existing `WelcomeScreen` / `AppDest.Welcome` = home feed. First-launch teaching UI is **`FirstLaunchScreen`** (do not rename home).

---

### Task 1: MonthDots — any unfinished entry counts

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/util/MonthDots.kt`
- Create: `android-app/src/test/java/com/yuyue/app/util/MonthDotsTest.kt`

**Interfaces:**
- Consumes: `EntryEntity.done`, existing `DayDot`
- Produces: `MonthDots.forEntries(entries, nowMillis)` ignores `MemoryKind`; pending if any `!done`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.yuyue.app.util

import com.yuyue.app.data.local.EntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthDotsTest {
    @Test
    fun lifeDiaryUndone_isRed() {
        val now = 1_000_000L
        val e = EntryEntity(
            id = "1",
            notebookId = "n",
            entryKind = "note",
            status = "active",
            subjectKey = "",
            polarity = "",
            title = "晚餐",
            body = "",
            rawTranscript = "",
            startAtMillis = now - 60_000,
            endAtMillis = null,
            statedAtMillis = now,
            updatedAtMillis = now,
            done = false,
            diaryType = "life",
        )
        assertEquals(DayDot.Red, MonthDots.forEntries(listOf(e), now))
    }

    @Test
    fun allDone_isGreen() {
        val now = 1_000_000L
        val e = EntryEntity(
            id = "1",
            notebookId = "n",
            entryKind = "note",
            status = "active",
            subjectKey = "",
            polarity = "",
            title = "股票",
            body = "",
            rawTranscript = "",
            startAtMillis = now - 60_000,
            endAtMillis = null,
            statedAtMillis = now,
            updatedAtMillis = now,
            done = true,
            diaryType = "life",
        )
        assertEquals(DayDot.Green, MonthDots.forEntries(listOf(e), now))
    }
}
```

(If `EntryEntity` constructor differs, match fields from `WelcomeFeedTest.entry`.)

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :android-app:testDebugUnitTest --tests com.yuyue.app.util.MonthDotsTest`

Expected: FAIL — life undone currently treated as Green (because `MemoryKind.isTaskOrEvent` is false).

- [ ] **Step 3: Implement**

In `MonthDots.kt`, change `forEntries` to:

```kotlin
fun forEntries(entries: List<com.yuyue.app.data.local.EntryEntity>, nowMillis: Long): DayDot {
    val pending = entries.any { !it.done }
    return if (pending) DayDot.Red else DayDot.Green
}
```

Remove unused `MemoryKind` import. Keep `nowMillis` parameter for call-site compatibility.

- [ ] **Step 4: Run tests — expect PASS**

Run: `.\gradlew.bat :android-app:testDebugUnitTest --tests com.yuyue.app.util.MonthDotsTest`

- [ ] **Step 5: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/util/MonthDots.kt android-app/src/test/java/com/yuyue/app/util/MonthDotsTest.kt
git commit -m "fix: calendar day dots count any unfinished diary"
```

---

### Task 2: Always show status dots on home / search rows

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/ui/WelcomeScreen.kt` (`WelcomeFeedRow` ~780–843)

**Interfaces:**
- Consumes: `entry.done`, `onToggleDone`
- Produces: every row shows green/pending 8.dp dot in 40.dp hit target (same layout as today when `showDone` was true)

- [ ] **Step 1: Remove the gate**

In `WelcomeFeedRow`, delete:

```kotlin
val showDone = MemoryKind.isTaskOrEvent(entry, System.currentTimeMillis())
```

Always render the dot `Box` (same as current `if (showDone) { ... }` body). Remove unused `MemoryKind` import if nothing else uses it in the file.

- [ ] **Step 2: Sanity check DayTimeline**

`DayTimelineScreen.TimelineRow` already always shows the dot — no change unless a gate was added later.

- [ ] **Step 3: Build**

Run: `.\gradlew.bat :android-app:assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/ui/WelcomeScreen.kt
git commit -m "fix: show done/pending dots on every home feed row"
```

---

### Task 3: ProfileStore welcomeSeen + ViewModel state

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/data/ProfileStore.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt`

**Interfaces:**
- Produces:
  - `ProfileStore.isWelcomeSeen(): Boolean` — default `false`
  - `ProfileStore.markWelcomeSeen()`
  - `AppViewModel.welcomeSeen: StateFlow<Boolean>`
  - `AppViewModel.markWelcomeSeen()` — persists + updates flow

- [ ] **Step 1: Add prefs keys** (mirror backup nudge pattern)

```kotlin
fun isWelcomeSeen(): Boolean =
    prefs.getBoolean(KEY_WELCOME_SEEN, false)

fun markWelcomeSeen() {
    prefs.edit().putBoolean(KEY_WELCOME_SEEN, true).apply()
}

// companion:
private const val KEY_WELCOME_SEEN = "welcome_seen"
```

- [ ] **Step 2: Expose on ViewModel**

Near `_backupNudge` / profile fields:

```kotlin
private val _welcomeSeen = MutableStateFlow(profileStore.isWelcomeSeen())
val welcomeSeen: StateFlow<Boolean> = _welcomeSeen

fun markWelcomeSeen() {
    profileStore.markWelcomeSeen()
    _welcomeSeen.value = true
}
```

- [ ] **Step 3: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/data/ProfileStore.kt android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt
git commit -m "feat: persist first-launch welcomeSeen flag"
```

---

### Task 4: FirstLaunchScreen + MainActivity gate

**Files:**
- Create: `android-app/src/main/java/com/yuyue/app/ui/FirstLaunchScreen.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/MainActivity.kt`

**Interfaces:**
- Consumes: `vm.welcomeSeen`, `vm.markWelcomeSeen()`, `vm.message` toast path (or local snackbar)
- Produces: full-screen teaching UI before home stack

- [ ] **Step 1: Create `FirstLaunchScreen`**

Composable signature:

```kotlin
@Composable
fun FirstLaunchScreen(
    onStart: () -> Unit,
    onMicHint: () -> Unit,
)
```

Layout (single screen):
1. Short welcome line (e.g. `用嘴记下你的时间`)
2. Large mic `Button` / circular control → `onMicHint()`
3. Bottom primary `Button` text `开始使用` → `onStart()`

Reuse `WelcomeAtmosphere` / existing colors if easy; otherwise Material3 surface + primary. Do not wire `SpeechTranscriber`.

- [ ] **Step 2: Gate in `MainActivity`**

Collect `welcomeSeen`. Structure:

```kotlin
val welcomeSeen by vm.welcomeSeen.collectAsState()
when {
    !welcomeSeen -> FirstLaunchScreen(
        onStart = { vm.markWelcomeSeen() },
        onMicHint = {
            vm.markWelcomeSeen()
            // emit toast via existing toast helper if available, else:
            // show a one-shot message: 「到首页按住说一句」
        },
    )
    editingEntry != null -> ...
    // existing when branches
}
```

Prefer adding a small ViewModel helper:

```kotlin
fun completeFirstLaunch(showMicHint: Boolean) {
    markWelcomeSeen()
    if (showMicHint) emitToast("到首页按住说一句")
}
```

Then mic → `completeFirstLaunch(true)`, start → `completeFirstLaunch(false)`.

- [ ] **Step 3: Build + push**

```powershell
.\gradlew.bat :android-app:assembleDebug
C:\Android\Sdk\platform-tools\adb.exe push android-app\build\outputs\apk\debug\android-app-debug.apk /sdcard/Download/Yuyue.apk
```

Manual: clear app data or unset flag → see first launch → start → home; relaunch → no gate.

- [ ] **Step 4: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/ui/FirstLaunchScreen.kt android-app/src/main/java/com/yuyue/app/MainActivity.kt android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt
git commit -m "feat: one-time teaching first-launch screen"
```

---

### Task 5: DAO + repository batch soft-delete and observe-by-notebook

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/data/local/Daos.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/data/YuyueRepository.kt`

**Interfaces:**
- Produces:
  - `EntryDao.observeActiveByNotebook(notebookId: String): Flow<List<EntryEntity>>`
  - `EntryDao.softDeleteByIds(ids: List<String>, now: Long)`
  - `YuyueRepository.observeActiveEntries(notebookId: String): Flow<List<EntryEntity>>`
  - `YuyueRepository.deleteEntries(ids: List<String>)` — soft-delete each (or batch query), remove vector index per id like `deleteEntry`

- [ ] **Step 1: DAO queries**

```kotlin
@Query(
    """
    SELECT * FROM entries
    WHERE notebookId = :notebookId
    AND deletedAtMillis IS NULL
    AND status = 'active'
    ORDER BY startAtMillis DESC
    """,
)
fun observeActiveByNotebook(notebookId: String): Flow<List<EntryEntity>>

@Query(
    """
    UPDATE entries SET deletedAtMillis = :now, updatedAtMillis = :now
    WHERE id IN (:ids) AND deletedAtMillis IS NULL
    """,
)
suspend fun softDeleteByIds(ids: List<String>, now: Long)
```

- [ ] **Step 2: Repository**

```kotlin
fun observeActiveEntries(notebookId: String): Flow<List<EntryEntity>> =
    entryDao.observeActiveByNotebook(notebookId)

suspend fun deleteEntries(ids: List<String>) {
    if (ids.isEmpty()) return
    val now = System.currentTimeMillis()
    // Prefer per-id deleteEntry to keep revision/vector behavior identical:
    ids.forEach { deleteEntry(it) }
}
```

Using loop of existing `deleteEntry` is acceptable for v1 (correct revisions/index). Skip raw `softDeleteByIds` if unused — or use it only if `deleteEntry` is too slow; prefer correctness via `deleteEntry` loop and **omit** unused DAO update if so.

**Decision for implementer:** implement `deleteEntries` as `ids.forEach { deleteEntry(it) }` only; still add `observeActiveByNotebook`. Add `softDeleteByIds` only if you also switch `deleteEntries` to use it **and** still call `vectorStore.removeIndex` + revision write for each id.

Recommended minimal:

```kotlin
suspend fun deleteEntries(ids: List<String>) {
    ids.distinct().forEach { deleteEntry(it) }
}
```

- [ ] **Step 3: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/data/local/Daos.kt android-app/src/main/java/com/yuyue/app/data/YuyueRepository.kt
git commit -m "feat: observe and batch-delete notebook entries"
```

---

### Task 6: ViewModel APIs for workspace diary management

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt`

**Interfaces:**
- Produces:
  - `_workspaceEntries: StateFlow<List<EntryEntity>>`
  - `fun loadWorkspaceEntries(notebookId: String)` — cancel previous job; collect `repository.observeActiveEntries`
  - `fun clearWorkspaceEntries()`
  - `fun deleteEntries(ids: List<String>)` — call repo, toast `已移入废纸篓`, `refreshMonth()`

- [ ] **Step 1: Add state + job**

```kotlin
private var workspaceEntriesJob: Job? = null
private val _workspaceEntries = MutableStateFlow<List<EntryEntity>>(emptyList())
val workspaceEntries: StateFlow<List<EntryEntity>> = _workspaceEntries

fun loadWorkspaceEntries(notebookId: String) {
    workspaceEntriesJob?.cancel()
    workspaceEntriesJob = viewModelScope.launch {
        repository.observeActiveEntries(notebookId).collect { _workspaceEntries.value = it }
    }
}

fun clearWorkspaceEntries() {
    workspaceEntriesJob?.cancel()
    _workspaceEntries.value = emptyList()
}

fun deleteEntries(ids: List<String>) {
    if (ids.isEmpty()) return
    viewModelScope.launch {
        runCatching {
            repository.deleteEntries(ids)
            emitToast(if (ids.size == 1) "已移入废纸篓" else "已移入废纸篓 ${ids.size} 则")
            refreshMonth()
        }.onFailure { emitToast(it.message ?: "无法删除") }
    }
}
```

- [ ] **Step 2: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt
git commit -m "feat: viewmodel workspace entry list and batch delete"
```

---

### Task 7: WorkspaceDetailScreen + WorkspaceEntriesScreen + Settings wiring

**Files:**
- Create: `android-app/src/main/java/com/yuyue/app/ui/WorkspaceDetailScreen.kt`
- Create: `android-app/src/main/java/com/yuyue/app/ui/WorkspaceEntriesScreen.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/ui/SettingsScreen.kt`
- Modify: `android-app/src/main/java/com/yuyue/app/MainActivity.kt`

**Interfaces:**
- Consumes: `vm.updateNotebook`, `vm.loadWorkspaceEntries`, `vm.workspaceEntries`, `vm.deleteEntries`, `vm.clearWorkspaceEntries`
- Settings: replace inline edit dialog with `onEditWorkspace: (NotebookEntity) -> Unit`

- [ ] **Step 1: `WorkspaceDetailScreen`**

```kotlin
@Composable
fun WorkspaceDetailScreen(
    notebook: NotebookEntity,
    onBack: () -> Unit,
    onSave: (name: String, color: String?) -> Unit,
    onManageEntries: () -> Unit,
)
```

- Fields: name `OutlinedTextField`, color swatches (copy from current Settings edit dialog accents: `NotebookAccent.*`)
- Buttons: 保存, 管理日记, 返回
- On 保存: `onSave(name, color)` then stay or `onBack` (prefer stay + toast from VM)

- [ ] **Step 2: `WorkspaceEntriesScreen`**

```kotlin
@Composable
fun WorkspaceEntriesScreen(
    notebookName: String,
    entries: List<EntryEntity>,
    onBack: () -> Unit,
    onOpenEntry: (EntryEntity) -> Unit,
    onDeleteSelected: (List<String>) -> Unit,
)
```

- `var selected by remember { mutableStateOf(setOf<String>()) }`
- LazyColumn rows: checkbox + title + time
- Top bar: `已选 N` when non-empty
- Bottom: `移入废纸篓` enabled when `selected.isNotEmpty()` → confirm via `YuyueAlertDialog` → `onDeleteSelected(selected.toList())` → clear selection
- `LaunchedEffect`/`DisposableEffect` for load/clear owned by MainActivity caller

- [ ] **Step 3: Extend `AppOverlay`**

```kotlin
private sealed class AppOverlay {
    data object Settings : AppOverlay()
    data object Trash : AppOverlay()
    data class WorkspaceDetail(val notebookId: String) : AppOverlay()
    data class WorkspaceEntries(val notebookId: String) : AppOverlay()
}
```

(If current is `enum`, convert to sealed class and update all comparisons.)

BackHandler:
- WorkspaceEntries → WorkspaceDetail
- WorkspaceDetail → Settings
- Trash → Settings (unchanged)

- [ ] **Step 4: Settings「编辑」**

```kotlin
TextButton(onClick = { onEditWorkspace(nb) }) { Text("编辑") }
```

Remove the old `editingNotebook` dialog block for name/color (moved to detail). Keep delete dialog.

Add parameter:

```kotlin
onEditWorkspace: (NotebookEntity) -> Unit,
```

- [ ] **Step 5: Wire MainActivity**

When opening WorkspaceDetail / Entries, find notebook by id from `notebooks`. Call `vm.loadWorkspaceEntries` when entering Entries; `vm.clearWorkspaceEntries` on leave.

- [ ] **Step 6: Build + push**

```powershell
.\gradlew.bat :android-app:assembleDebug
C:\Android\Sdk\platform-tools\adb.exe push android-app\build\outputs\apk\debug\android-app-debug.apk /sdcard/Download/Yuyue.apk
```

Manual checklist from spec acceptance.

- [ ] **Step 7: Commit** (skip if no git)

```bash
git add android-app/src/main/java/com/yuyue/app/ui/WorkspaceDetailScreen.kt android-app/src/main/java/com/yuyue/app/ui/WorkspaceEntriesScreen.kt android-app/src/main/java/com/yuyue/app/ui/SettingsScreen.kt android-app/src/main/java/com/yuyue/app/MainActivity.kt
git commit -m "feat: workspace detail and batch trash for diaries"
```

---

## Spec coverage self-check

| Spec requirement | Task |
|------------------|------|
| First launch single-screen teaching | 4 |
| Mic illustrative + toast | 4 |
| 「开始使用」 + `welcomeSeen` | 3, 4 |
| Workspace edit name/color in detail | 7 |
| Manage diaries multi-select → trash | 5, 6, 7 |
| Soft-delete / trash only | 5 |
| Keep ≥1 notebook | unchanged Settings delete |
| Every feed row status dot | 2 |
| Calendar any-undone | 1 |
| No overwrite of hand-edited settings slogans | 7 (only edit routing) |
| APK push | 4, 7 |

## Placeholder scan

No TBD / “implement later” left in tasks.

## Type consistency

- Flag: `welcomeSeen` / `isWelcomeSeen` / `markWelcomeSeen` / `completeFirstLaunch`
- Batch: `deleteEntries(ids: List<String>)` on repo + VM
- Overlay: `WorkspaceDetail(notebookId)` / `WorkspaceEntries(notebookId)`
- Screen: `FirstLaunchScreen` ≠ home `WelcomeScreen`

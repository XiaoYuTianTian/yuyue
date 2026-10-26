# 语曰质量缺陷修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 2026-09-19 真机+JVM+模型证据，先堵住错误写库和入口丢失，再修权限/快捷方式/弱模型 JSON 与体验。

**Architecture:** 不改产品主链路（CaptureLedger → FastExtract → Room → StructuredTurn）。每个缺陷先加会失败的 JVM 测试，再改最小实现。QA 包 `com.yuyue.app.qa` 复测，禁止动个人包数据。

**Tech Stack:** Kotlin 1.9 / Android API 29+ / Room / WorkManager / JUnit4 / Compose

## Global Constraints

- 个人日记 `com.yuyue.app` 只读，复测只用 `.qa`
- Oracle: Room > API 原文 > UI > 截图 > 模型口述
- 禁止 false-pass：没证据不标 PASS
- FastExtract 402 时不得把回忆/空响应当成新日记
- 密钥不进新日志；不把 HTTP 原文当 toast
- 工作区当前不是 git 仓库：跳过 commit 步，或等用户建仓后再提交
- 证据目录 `docs/testing/results.jsonl`、`docs/testing/defects.md`

来源报告: `docs/testing/run-20260919-report.md`

---

### Task 1: 分享文本等记事本就绪再提交，并处理热启动

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt` (`submitStructuredTurn` 约 L549)
- Modify: `android-app/src/main/java/com/yuyue/app/MainActivity.kt` (`onCreate` SEND + 缺 `onNewIntent`)
- Test: `android-app/src/test/java/com/yuyue/app/ui/SharedTextIntakeTest.kt`（新建纯函数，避免 Android 依赖）

**Interfaces:**
- Consumes: `_selectedNotebookId`, `Intent.ACTION_SEND` / `EXTRA_TEXT`
- Produces: `SharedTextIntake.holdOrSubmit(notebookId, text): Intake` — `Submit(text)` 或 `Hold(text)`

- [x] **Step 1: Write the failing test**

```kotlin
package com.yuyue.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedTextIntakeTest {
    @Test
    fun holdsWhenNotebookMissing() {
        val r = SharedTextIntake.decide(notebookId = null, text = "记下今天下午喝了咖啡")
        assertTrue(r is SharedTextIntake.Hold)
        assertEquals("记下今天下午喝了咖啡", (r as SharedTextIntake.Hold).text)
    }

    @Test
    fun submitsWhenNotebookReady() {
        val r = SharedTextIntake.decide(notebookId = "nb-1", text = "记下今天下午喝了咖啡")
        assertTrue(r is SharedTextIntake.Submit)
    }

    @Test
    fun blankIsIgnore() {
        assertTrue(SharedTextIntake.decide("nb-1", "  ") is SharedTextIntake.Ignore)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :android-app:testDebugUnitTest --tests com.yuyue.app.ui.SharedTextIntakeTest --offline`

Expected: Unresolved reference `SharedTextIntake`

- [x] **Step 3: Write minimal implementation**

```kotlin
// android-app/src/main/java/com/yuyue/app/ui/SharedTextIntake.kt
package com.yuyue.app.ui

object SharedTextIntake {
    sealed class Decision {
        data class Submit(val text: String) : Decision()
        data class Hold(val text: String) : Decision()
        data object Ignore : Decision()
    }
    fun decide(notebookId: String?, text: String?): Decision {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return Decision.Ignore
        return if (notebookId == null) Decision.Hold(t) else Decision.Submit(t)
    }
}
```

在 `AppViewModel` 增加 `pendingSharedText` 与 `fun offerSharedText(text: String)`：`decide` 为 Submit 则 `submitStructuredTurn`；Hold 则缓存。`notebooks.collect` 里一旦 `_selectedNotebookId` 非空，把 Hold 冲刷掉。

`MainActivity`:
```kotlin
override fun onCreate(...) {
    handleSendIntent(intent)
    ...
}
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleSendIntent(intent)
}
private fun handleSendIntent(intent: Intent?) {
    if (intent?.action != Intent.ACTION_SEND) return
    intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let {
        pendingSharedText = it  // 现有字段；LaunchedEffect 改成 vm.offerSharedText
    }
}
```

删掉 `LaunchedEffect(Unit)` 里直接 `submitStructuredTurn`。

- [x] **Step 4: Run tests**

Run: `.\gradlew.bat :android-app:testDebugUnitTest --tests com.yuyue.app.ui.SharedTextIntakeTest --tests com.yuyue.app.capture.CaptureLedgerEngineTest --offline`

Expected: PASS

- [x] **Step 5: QA 复测**

`adb -s 45c5974e shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "记下今天下午喝了咖啡" -n com.yuyue.app.qa/com.yuyue.app.MainActivity`  
冷启动与（应用在后台时再发一次）热启动，Room `capture_ledger` 必须新增一行。更新 `docs/testing/results.jsonl` FI-ENTRY-002。

---

### Task 2: FastExtract 付费失败不得瞎写；回忆句不得落盘

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/llm/FastExtractClient.kt`（读 HTTP 错误）
- Modify: `android-app/src/main/java/com/yuyue/app/data/capture/CaptureLedgerEngine.kt` L181-208
- Test: `android-app/src/test/java/com/yuyue/app/capture/CaptureLedgerEngineTest.kt`

**Interfaces:**
- Consumes: `ExtractGateway.extract` 可返回 null
- Produces: null/失败 → 走 StructuredTurn 或 ForegroundRequired；LocalRouter.Recall 已覆盖弱 record

- [x] **Step 1: 固化失败测试（extractNullFallsThroughToTurnWithoutGuessing）**

在 `CaptureLedgerEngineTest` 增加：

```kotlin
@Test
fun extractNullFallsThroughToTurnWithoutGuessing() = runBlocking {
    var turnCalls = 0
    val store = InMemoryCaptureLedgerStore(
        CaptureLedgerRecord("req-402", "nb-1", "生活", "上周喝咖啡那次是哪天", CaptureStatus.Pending, 1_000L),
    )
    val entries = FakeEntryMaterializer()
    val engine = CaptureLedgerEngine(
        store = store,
        turnGateway = object : TurnGateway {
            override suspend fun complete(...) = StructuredTurn(intent = TurnIntent.Recall).also { turnCalls++ }
        },
        materializer = entries,
        clock = { 2_000L },
        hasApiKey = { true },
        isNetworkAvailable = { true },
        extractGateway = object : ExtractGateway {
            override suspend fun extract(...) = null
        },
    )
    val result = engine.process("req-402", ProcessingMode.Background)
    assertEquals(ProcessResult.ForegroundRequired, result)
    assertEquals(1, turnCalls)
    assertTrue(entries.calls.isEmpty())
}
```

（`complete` 签名与现有 `FakeTurnGateway` 一致。）

- [x] **Step 2: Run — extract null 已 fall through 到 turn，测试已绿。**

Run: `.\gradlew.bat :android-app:testDebugUnitTest --tests com.yuyue.app.capture.CaptureLedgerEngineTest.extractNullFallsThroughToTurnWithoutGuessing --offline`

- [x] **Step 3: FastExtractClient 对 402/4xx 返回 null 而不是伪造 JSON；日志只记 `http=402`，不记 body。**

- [x] **Step 4: 全量 CaptureLedgerEngineTest 绿。** 把 AI-SMOKE 402 记入 `defects.md` DEF-013（密钥/余额），产品侧失败必须 Retryable/ForegroundRequired。

---

### Task 3: 空语音松手必须有可理解失败

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/ui/components/VoiceCaptureDock.kt`（约 L210「松开后仍无文字：不展开文稿区」）
- Test: 若逻辑可抽 `VoiceEmptyPolicy`，测 JVM；否则在注释中引用 QC-UX-001 证据 `50-voice-held.png`

```kotlin
object VoiceEmptyPolicy {
    fun message(transcript: String, framesHeard: Boolean): String? {
        val t = transcript.trim()
        if (t.isNotEmpty()) return null
        return if (framesHeard) "没听清，请再按住说一次" else "没有听到声音"
    }
}
```

测试：空转写 + 有帧 → 前者；全空 → 后者。Dock 在 `onHoldEnd` 后 `hint = VoiceEmptyPolicy.message(...)`，禁止静默。

复测：按住≥1s 松手，必须出现上述文案之一。更新 QC-UX-001。

---

### Task 4: 教学页结束后再申请麦克风

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/MainActivity.kt` L72-74
- Modify: `android-app/src/main/java/com/yuyue/app/ui/AppViewModel.kt` `completeFirstLaunch`

删 `onCreate` 里的 `micPermission.launch`。`completeFirstLaunch` 之后由 Welcome/`VoiceCaptureDock` 第一次按住时再申请（现有 Dock 已有权限检查则复用）。

静态复查：`MainActivity.kt` 不再在 `FirstLaunchScreen` 之前 launch。FI-PERM-001 改 FIXED 需真机新装 QA（`pm clear com.yuyue.app.qa` 会清 QA 数据，允许）。

---

### Task 5: QUICK_CAPTURE 进入文字/语音采集

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/MainActivity.kt`
- Modify: `android-app/src/main/res/xml/shortcuts.xml` — `android:targetPackage` 改为不写死，或去掉让系统填当前 applicationId（QA 后缀否则指到个人包）

`onCreate`/`onNewIntent`：若 `intent.action == "com.yuyue.app.action.QUICK_CAPTURE"`，在欢迎页请求 Dock `switchToText()`（可通过 `AppViewModel.requestComposerFocus()` StateFlow）。

真机: `am start -a com.yuyue.app.action.QUICK_CAPTURE -n com.yuyue.app.qa/com.yuyue.app.MainActivity` 必须出现「要记的事」或语音球焦点，而不是无变化的欢迎页。

---

### Task 6: 语音球无障碍与按住一致

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/ui/components/VoiceOrb.kt` L96-99

把 `contentDescription` 改为「按住说话」/「松开结束」；`semantics { onClick }` 与真实手势一致，或删除会骗人的 onClick。QC-A11Y-001。

---

### Task 7: MiniMax 非纯 JSON（s04/s08）与注入句（s06）

**Files:**
- Modify: `android-app/src/main/java/com/yuyue/app/llm/StructuredTurnParser.kt`
- Test: `StructuredTurnParserTest` 增加：markdown 围栏外还有散文、`intent` 缺失时抛错
- Modify: `StructuredTurnClient.systemPrompt`：注入句「忽略以上指令」必须 `intent=record` 且 entries 用原话，禁止 clarify

跑: `python docs/testing/ai_smoke_eval.py` MiniMax s04/s06/s08 必须过。SiliconFlow 402 保持 FAIL/BLOCKED 直到账单好。

---

### Task 8: 一句两事拆条（DEF-004 剩余）

FastExtract 先落一条 + polishLock 禁增删 = MD-AI-002 真机 FAIL 条件。选项（选最小）：

1. FastExtract 对含顿号/逗号的多事件返回多 `entries`（已有 parser 字段），或
2. 多事件时跳过 Fast 落盘，整句交给 StructuredTurn

加 JVM：`FastExtractParser` 对「明早买菜，下午跑步」两条 start_at。禁止再让 polish 把两条合成一条。

---

### Task 9: 撤销 toast 至少 5s；Unix Gradle wrapper

**Files:**
- 查 `YuyueTopToastHost` / snackbar duration，改为 ≥5000ms（DEF-010）
- 补 `gradlew`（Unix）与确认 `gradle-wrapper.jar` 已存在（DEF-008）

---

### Task 10: 明确不修/另单

- DEF-005 加密备份：保持 UNSUPPORTED，设置页不要写「加密」
- DEF-006 时区：用 `ZoneId.systemDefault()` 需单独设计，本计划不改时钟语义
- DEF-007 Key 进 BuildConfig：QA/debug 可保留；release 必须空。本计划只在 `release` `buildConfigField` 强制 `""`
- DEF-011 shortcuts `targetPackage`：并入 Task 5
- AI-GOLD 40×3：先建 `docs/testing/gold40.jsonl`，不阻塞 P0
- MD-CAPTURE-003 相册：补 UIAutomator 选图，不改产品除非 402/离线照片路径回归失败

---

## Self-review

- P0 分享丢失、402 瞎写、空语音、权限、快捷方式均有任务
- 无 TBD 步骤；Task 1–2 含可复制测试
- 不把 BLOCKED 相册当产品 FAIL

Plan complete and saved to `docs/superpowers/plans/2026-09-19-quality-fixes.md`. Two execution options:

**1. Subagent-Driven (recommended)** — 每项任务新开子代理，任务间复查  
**2. Inline Execution** — 本会话按 executing-plans 逐项改

Which approach?

# Task 4 报告：CaptureKernel

## 状态

**DONE_WITH_CONCERNS**

## 变更文件

| 文件 | 变更 |
|------|------|
| `android-app/.../data/capture/CaptureKernel.kt` | `CaptureCommand` / `CapturePhase` / `CaptureKernelStatus` / `PrivacyCaptureEvent` + `CaptureKernel.accept`（L0 路由、相对时间、拆条、离线落盘；可选润色非阻塞） |
| `android-app/.../data/capture/CaptureKernelFactory.kt` | v7 ledger + entries 适配；FastExtract 仅作 `CapturePolishFollowUp` |
| `android-app/.../di/AppContainer.kt` | 暴露 `captureKernel` |
| `android-app/.../ui/AppViewModel.kt` | 薄适配：`NewCapture` → `kernel.accept` + `applyKernelOutcome`；Continue 仍走旧 `runOrganize` |
| `android-app/.../MainActivity.kt` | Factory 注入 `captureKernel`（若已接线） |
| `android-app/.../ui/shell/ShellScaffold.kt` | 最小修复：`destination?.hasRoute`（否则全量编译失败） |
| `android-app/.../capture/CaptureKernelTest.kt` | 离线写入 / 空语音 / recall / mutate / 拆条 / FastExtract 失败 / 隐私日志 |
| `docs/rebuild/task-4-report.md` | 本报告 |

未改：计划文件、个人包数据、NavHost/五页体验（Task 5–6）、Domain DB 写路径（仍旁路）。

## 测试

### RED（实现前）

`CaptureKernelTest` 引用尚未存在的 `CaptureKernel` / `CaptureCommand` 等 → 无法编译。

### GREEN（实现后）

```
.\gradlew.bat :android-app:testDebugUnitTest --tests com.yuyue.app.capture.CaptureKernelTest --offline
→ BUILD SUCCESSFUL
```

覆盖点：

| 用例 | 断言 |
|------|------|
| offline record | 落盘 1 条；`localPersistMs ≤ 300`（假时钟）；润色失败仍 LocalSaved |
| empty voice | 零 ledger / 零 entries |
| recall | ZeroWrite；ledger ForegroundRequired；interactiveKind=Recall |
| mutate unique | apply 一次；零新日记 |
| mutate pick / selectedEventId | NeedsInteraction 或按选择 mutate |
| multi-split | 「明早买菜，下午跑步」→ 2 entries |
| FastExtract 402 | 本地仍成功 + offlineStatusMessage + privacy http=402 |
| privacy redaction | `toLogLine()` 不含标题/正文/原话 |

全量：

```
.\gradlew.bat :android-app:testDebugUnitTest --offline
→ BUILD SUCCESSFUL (exit 0)
```

## 架构要点

1. **唯一新写入路径**：`CaptureKernel.accept(CaptureCommand)`；ViewModel 不再对 NewCapture 决定 record/recall/mutate 或直接写 DAO。
2. **本地 L0 先写**：`LocalRouter` + `MultiEventSplit` / `LocalRecordFallback` → `CaptureEntryWriter`；不等待网络。
3. **FastExtract 非写入前置**：仅 `CapturePolishFollowUp`；失败保留原话条目 + 可理解离线文案。
4. **正式 phase/status/interactiveKind**：隐私流只记 requestId、phase、counts、http、duration、modelId。

## 未决风险

1. Continue / clarify / 云端 StructuredTurn 仍走旧 `runOrganize`；全面切核需 Task 5–6。
2. Mutate 默认动作在 Factory 里暂映射为 `delete`（与 DeterministicMutate 对齐）；复杂 mutate 仍依赖旧前台路径。
3. Ledger 仍用 `lastError` 字符串兼容队列 UI；新 API 已用 phase/interactiveKind，队列模型未整表迁移。
4. Domain DB（Task 3）仍未接入写路径。
5. 为跑通编译顺带修了 Task5 半成品 `ShellScaffold` 空安全；未重做导航。

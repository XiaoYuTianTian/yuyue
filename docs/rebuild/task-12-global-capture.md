# Task 12：全局 Capture 层

## 状态

**DONE_WITH_CONCERNS**

## 目标

Capture 是 ShellScaffold 上唯一的全局 overlay，跨 Home / Time / Recall 共用；不在各 Tab 各挂一份 `VoiceCaptureDock`。Vault 不显示采集球；Onboarding 自有按住说话，不受影响。

## 变更文件

| 文件 | 变更 |
|------|------|
| `ui/shell/ShellScaffold.kt` | `CaptureOverlaySlot` 挂真实 `VoiceCaptureDock`；Vault 仅队列条；底栏上方共用 |
| `ui/shell/YuyueNavHost.kt` | 向 ShellScaffold 传入 `appVm` + `transcriber`；Tab destination 不再传 dock |
| `ui/shell/ShellDestinations.kt` | Home/Time/Recall 去掉 `transcriber` |
| `ui/shell/ShellViewModels.kt` | CaptureViewModel 注释：壳层会话 + AppVM 提交/吐司 |
| `ui/shell/screens/HomePaperScreen.kt` | 移除 `VoiceCaptureDock` / 重复 `CaptureQueueBar` |
| `ui/shell/screens/TimePaperScreen.kt` | 同上 |
| `ui/shell/screens/RecallPaperScreen.kt` | 同上 |
| `ui/components/VoiceCaptureDock.kt` | 新增 `consumeNavigationBars`（壳内在 NavigationBar 之上时关闭双重 inset） |
| `data/capture/CaptureLedgerEngine.kt` | extract=null 且非 Record/Cloud 时 fall through 到 StructuredTurn（修复失败单测） |
| `docs/rebuild/task-12-global-capture.md` | 本报告 |

未改：计划文件、Onboarding 按住说话、DayTimeline / Welcome / Calendar 等 legacy 屏自有 dock、个人包数据。

## 行为说明

1. **单一 dock**：Home / Time / Recall 切换时 Compose 节点不重建，草稿/语音态留在壳层。
2. **提交**：`appVm.submitStructuredTurn`（吐司、澄清、pending draft 仍走 AppViewModel）；`CaptureViewModel` 负责队列观察、`composerFocusNonce`、notebook 绑定与 `accept`/`offerSharedText`。
3. **队列**：壳 `CaptureQueueBar` 接 `captureVm.activeQueue`，动作委派 `appVm.discard/retry/continue/pause/resumeCapture`；不再在 Home 再挂一份。
4. **Vault**：隐藏 dock；若有活跃队列仍显示 `CaptureQueueBar`。
5. **Onboarding**：仍用自身 hold-to-speak → `submitStructuredTurn`，不经壳 overlay。

## 测试

```
.\gradlew.bat :android-app:compileDebugKotlin :android-app:testDebugUnitTest --offline
→ BUILD SUCCESSFUL
```

重点覆盖：

| 用例 | 断言 |
|------|------|
| `extractNullFallsThroughToTurnWithoutGuessing` | extract=null + Recall 文案 → 调用 turn，ForegroundRequired，不落盘（已修复） |
| 全量 `testDebugUnitTest` | 编译与单测全绿 |

## 未决风险

1. **Day / Welcome / Calendar legacy**：仍各自挂 dock；仅重建壳四 Tab 收敛。FEATURE_REBUILD_SHELL=false 时行为不变。
2. **语音回忆结果卡**：Welcome 曾在 dock `extraAbove` 放 `RecallResultCard`；壳层 Recall 纸面靠搜索列表 + `recallHits`，未在全局 dock 上再叠一张结果卡。
3. **双队列源**：壳观察 `CaptureViewModel.activeQueue`（ledger），动作写 AppViewModel；若两边 ledger 实例不一致会出现条不同步（当前均来自同一 application container）。
4. **`onExpandCapture`**：NavHost 仍接线 `captureVm.requestComposerFocus`，RecallDestination 暂未从纸面触发（保留 API）。

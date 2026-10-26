# Task 6 报告：核心屏幕（core-screens）

## 状态

**DONE_WITH_CONCERNS**

## 目标（plan §6）

- Onboarding：真实按住说一句 → 预览 → 第一条；麦克风权限在动作时解释并申请
- Home：语音球主角；今天 + 少量近期；记事本次级筛选；去掉「接下来/未完成」默认叙事
- Time：月历记忆地图 → 日胶囊 → 详情；模糊时间「约晚上」等，不造假分钟
- Recall：文字/语音共用 FTS 路径；结果为可追溯 `entryId` 的日期节点
- Entry / Vault：原话 + 整理 + 修订 + 媒体 + 来源；锁/备份/废纸篓/记事本/外观/导出/AI 高级

## 变更文件

| 文件 | 变更 |
|------|------|
| `util/FuzzyTimeLabel.kt` | 模糊时段标签（语音优先 / 小时分桶），永不输出 `HH:mm` |
| `ui/shell/OnboardingFlow.kt` | 纯状态机 Intro→Speak→Preview→Committing→Finished |
| `ui/shell/HomeFeedSplit.kt` | 今天 / 近期切片 + 字幕文案 |
| `ui/shell/screens/OnboardingScreen.kt` | 真实 SpeechTranscriber 按住 + 权限说明对话框 + 预览写入 |
| `ui/shell/screens/HomePaperScreen.kt` | 纸面首页：品牌语曰、今天/近期胶囊、次级记事本、VoiceCaptureDock |
| `ui/shell/screens/TimePaperScreen.kt` | 月历打点 → 当日 EventCapsule（模糊时间）→ Entry |
| `ui/shell/screens/RecallPaperScreen.kt` | 文字搜索 + 语音共用 `submitStructuredTurn`；苔线节点含 entryId |
| `ui/shell/screens/EntryPaperScreen.kt` | 原话 / 整理 / 修订 / 媒体 / 来源 |
| `ui/shell/screens/VaultPaperScreen.kt` | 锁(占位) / 备份 / 废纸篓 / 记事本 / 外观 / 导出 / AI |
| `ui/shell/ShellDestinations.kt` | 接线到新屏；Entry 用 EntryViewModel.revisions |
| `ui/shell/YuyueNavHost.kt` | Onboarding/Recall 传入 `transcriber` |
| `ui/shell/EntryViewModel.kt` | 加载 `listRevisions` |
| `ui/shell/ShellViewModels.kt` | HomeFeedSplit 辅助；注释更新 |
| `ui/shell/ShellScaffold.kt` | Capture 槽注释 |
| `res/values/strings.xml` | 补齐 widget / on_this_day 字符串（解除并行任务造成的资源链接失败） |
| `notify/OnThisDayWork.kt` | 修复 `onThisDay` 参数与可空 `rawTranscript`（编译门禁） |
| `test/.../FuzzyTimeLabelTest.kt` | 模糊标签单测 |
| `test/.../OnboardingFlowTest.kt` | 引导状态机 + HomeFeedSplit 单测 |
| `docs/rebuild/task-6-report.md` | 本报告 |

未改：计划文件、`LegacyNavHost` 行为（仍可用旧 Welcome/Settings）、个人包数据。

## 行为说明

1. **Onboarding**：CTA → 按住球体（先解释再申请 RECORD_AUDIO）→ 松手等最终转写 → 预览卡 → `submitStructuredTurn` + `completeFirstLaunch`。
2. **Home**：不再展示 WelcomeFilter「接下来/未完成」与 pending 摘要；默认「今天 · N 条已落轴」。
3. **Time**：有记忆日可点；胶囊时间用 `FuzzyTimeLabel`（如「约晚上」）。
4. **Recall / Capture**：仍走 `AppViewModel` + CaptureKernel 桥；壳不抢写路径。
5. **Vault**：应用锁为「即将推出」占位；外观接 `ThemeMode`；备份/导入沿用 MainActivity 回调。

## 测试

```
.\gradlew.bat :android-app:compileDebugKotlin :android-app:testDebugUnitTest --tests "com.yuyue.app.util.FuzzyTimeLabelTest" --tests "com.yuyue.app.ui.shell.OnboardingFlowTest" --tests "com.yuyue.app.ui.shell.HomeFeedSplitTest" --tests "com.yuyue.app.ui.shell.ShellRoutesTest" --offline
→ BUILD SUCCESSFUL
```

| 用例 | 断言 |
|------|------|
| `speechEveningBecomesApproxEvening` | 原话含「晚上」→「约晚上」 |
| `hourBucketWithoutSpeech` | 15 时 →「约下午」 |
| `neverEmitsClockMinutes` | 标签无 `:` |
| `introToSpeakToPreviewToWrite` | 状态机完整路径 |
| `blankPreviewStaysOnSpeak` | 空转写不进 Preview |
| `splitsTodayAndRecentWithoutUndoneNarrative` | 今天/近期切片与字幕 |

## 未决风险

1. **Capture 仍未统一进全局 overlay**：Home/Time/Recall 各挂 `VoiceCaptureDock`；`CaptureOverlaySlot` 仍是轻量占位。
2. **DayTimelineScreen 仍显示 `HH:mm`**：Time 主路径已模糊；从回忆跳日轴仍走旧屏。
3. **应用锁未实现**：Vault 仅占位，产品 §7 完整锁待后续。
4. **并行任务残留**：为通过编译补了 widget 字符串与 `OnThisDayWork` 小修；与 Task 6 无产品耦合。
5. **Onboarding 转写时序**：依赖 `listening/busy` 结束后读 `partial`；极短按仍可能「没听清」。

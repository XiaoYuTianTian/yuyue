# Task 5 报告：应用壳 / NavHost / 分屏 ViewModel

## 状态

**DONE_WITH_CONCERNS**

## 目标（plan §5）

- Navigation Compose typed routes 替换手写 `stack + overlay + viewingEntry`
- 结构：`Onboarding`；壳内 `Home` / `Time` / `Recall` / `Vault`；`Entry/{id}`；Capture 为全局层（非 Tab）
- 拆分 ViewModel；详情按 `entryId` Flow + `SavedStateHandle`
- `MainActivity` 仅 Intent / 导入导出 / Compose 宿主
- 可回退：`BuildConfig.FEATURE_REBUILD_SHELL`

## 变更文件

| 文件 | 变更 |
|------|------|
| `android-app/.../ui/shell/Routes.kt` | `@Serializable` typed routes；`ShellTabRoutes`（四 Tab，不含 Capture） |
| `android-app/.../ui/shell/YuyueNavHost.kt` | 重建 NavHost：Onboarding + nested Shell + Entry/Edit/Day/Trash/Workspace |
| `android-app/.../ui/shell/ShellScaffold.kt` | 底栏四 Tab + 全局 Capture overlay 槽 + 队列条 |
| `android-app/.../ui/shell/ShellDestinations.kt` | 复用现有屏为 destination（Task 6 再抛光） |
| `android-app/.../ui/shell/ShellViewModels.kt` | `Capture` / `Home` / `Time` / `Recall` / `Vault` ViewModel 桩 + `CaptureLegacyBridge` |
| `android-app/.../ui/shell/EntryViewModel.kt` | `Entry` / `EntryEdit`：`SavedStateHandle.toRoute` + `observeEntry`；`EntryDetailState` Loading/Missing/Present |
| `android-app/.../ui/LegacyNavHost.kt` | 旧手写导航抽出，供 flag=false 回退 |
| `android-app/.../MainActivity.kt` | Intent/导入导出宿主；`FEATURE_REBUILD_SHELL` 切换 `YuyueNavHost` / `LegacyNavHost` |
| `android-app/build.gradle.kts` | `buildConfigField FEATURE_REBUILD_SHELL = true` |
| `android-app/.../ui/shell/ShellRoutesTest.kt` | 路由序列化往返 + Tab 集合单测 |
| `docs/rebuild/task-5-report.md` | 本报告 |

未改：计划文件、个人包数据、屏幕视觉抛光（Task 6）、CaptureKernel 写路径（Task 4 负责；壳侧仅 legacy bridge）。

## 与 CaptureKernel 并行协调

- **未重写** `AppViewModel` 组织/写入逻辑；壳通过 `CaptureLegacyBridge` 把快捷采集/分享转发回 `AppViewModel`。
- `CaptureViewModel` 只持有草稿展开态与 ledger 队列观察；TODO 标注待内核 phase/status 接管 UI。
- `MainActivity` 仍向 `AppViewModel` 注入 `captureKernel`（Task 4）；壳不抢写路径。

## 测试

```
.\gradlew.bat :android-app:compileDebugKotlin :android-app:testDebugUnitTest --tests "com.yuyue.app.ui.shell.*" --offline
→ BUILD SUCCESSFUL
```

覆盖：

| 用例 | 断言 |
|------|------|
| `entryRoute_roundTrips` | `Routes.Entry` JSON 往返 |
| `entryEditRoute_roundTrips` | `Routes.EntryEdit` |
| `dayRoute_usesEpochDay` | epochDay 保留 |
| `workspaceRoutes_roundTrip` | Detail / Entries |
| `tabSingletons_areDistinctTypes` | 单例路由相等 |
| `shellTabRoutes_coversFourTabs` | 恰四 Tab，不含 Onboarding/Trash |
| `entryDetailState_loadingIsDistinctFromMissing` | Loading ≠ Missing |

## 启用 / 回退

- 默认 **`FEATURE_REBUILD_SHELL = true`** → `YuyueNavHost`
- 设为 `false` → `LegacyNavHost`（旧 stack/overlay/viewingEntry）

## 未决风险

1. **屏幕仍绑 `AppViewModel`**：Home/Time/Vault 等 destination 复用旧屏；分屏 VM 多为桩，真正驱动 UI 在 Task 6。
2. **Capture overlay 占位**：全局层有会话态与队列条；语音球仍在 Welcome 内，统一采集 UI 待 Task 6。
3. **Intent 路径**：分享/快捷采集仍直接进 `AppViewModel`；`CaptureViewModel` 草稿可能不同步（桥接在 NavHost 内 attach）。
4. **Continue / clarify**：仍走旧 `runOrganize`（见 Task 4 报告）；壳不解决写路径完整性。
5. **Domain DB**：读路径仍是 legacy `YuyueRepository` / `observeEntry`。

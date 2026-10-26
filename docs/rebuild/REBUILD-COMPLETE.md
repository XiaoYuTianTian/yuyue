# 重建收尾状态（Task 14）

## 代码侧：视为完成

相对计划 todo 与先前缺口，下列已落地：

| 项 | 证据 |
|----|------|
| Domain 旁路 + dual-write | `task-10-domain-cutover.md`，启动 `ensureDomainMigrated()` |
| CaptureKernel 独占写入 | `AppViewModel.acceptViaKernel` / `submitStructuredTurn` |
| 全局 Capture | `task-12-global-capture.md`，壳上唯一 Dock |
| §8 清旧路径 | `task-13-cleanup-legacy.md`（AgentLoop/TokenStore/OfflineSync/MutateSheet 已删） |
| 备份修订导入 | `importJson` 持久化 `revisionsToUpsert` |
| JVM 金标 / 单测 | `gold40` + `testDebugUnitTest` |

## 仍属设备/发布门禁（无法在本机代码会话“勾完”）

1. QA 模拟器 / ARM 真机矩阵（见 `verification-rollout.md` 空表）
2. QA 包完整备份→清空→恢复签字
3. 个人包升级前强制备份 + 一次升级验证
4. 生物识别系统对话框（依赖已预留；离线构建未强制拉 `androidx.biometric`）
5. Domain **读路径**切换与 release 默认 `FEATURE_DOMAIN_WRITE=true`（现 debug/qa 开、release 关）
6. Legacy Welcome/Day/Calendar Dock（仅 `FEATURE_REBUILD_SHELL=false` 回退路径）
7. 地图/天气/原声播放仍为本地 stub

## 产品结论

- **开发重建目标（代码）**：完成。
- **可推广发布（M4）**：需完成上表设备门禁后再开个人包。

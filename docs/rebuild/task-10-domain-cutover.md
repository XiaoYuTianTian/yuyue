# Task 10：Domain DB 写入切流（cutover）

## 状态

**DONE_WITH_CONCERNS**

## 目标

将 `YuyueDomainDatabase`（`yuyue-domain.db`）接入应用 DI，首启/显式迁移跑 `LegacyV7Importer`，新写入可 dual-write；UI 仍读 legacy `yuyue.db`。

## 变更文件

| 路径 | 说明 |
|------|------|
| `android-app/.../domain/DomainRepository.kt` | Domain 写适配：import 落库、`insertCaptureIfAbsent`（不覆盖 rawText）、修订 append-only、dual-write mirror |
| `android-app/.../domain/DomainDualWrite.kt` | CaptureKernel raw/entry 双写包装 |
| `android-app/.../domain/migrate/DomainCountSnapshot.kt` | 计数快照 + `DomainCountVerifier` |
| `android-app/.../domain/migrate/DomainMigrationGate.kt` | 空 domain ← 有数据 legacy 门闩 |
| `android-app/.../domain/migrate/DomainMigrationRunner.kt` | 导入 + 计数校验；软失败（不删 legacy） |
| `android-app/.../domain/migrate/LegacyV7SnapshotLoader.kt` | 从 v7 Room 装载 snapshot |
| `android-app/.../domain/local/DomainDaos.kt` | Capture `insertIgnore` / `updateMeta`；Media `count()` |
| `android-app/.../data/local/Daos.kt` | `CaptureLedgerDao.listIncomplete()` |
| `android-app/.../data/capture/CaptureKernelFactory.kt` | 可选 `domainRepository` + `domainWriteEnabled` 双写 |
| `android-app/.../di/AppContainer.kt` | 打开 domain DB、暴露 repository/runner、`ensureDomainMigrated()` |
| `android-app/.../YuyueApplication.kt` | 启动时后台跑迁移门闩 |
| `android-app/build.gradle.kts` | `FEATURE_DOMAIN_WRITE`：debug/qa `true`，release `false` |
| `android-app/.../domain/migrate/DomainMigrationGateTest.kt` | 门闩单测 |
| `android-app/.../domain/migrate/DomainCountVerifierTest.kt` | 计数校验 + empty←legacy |
| `android-app/.../domain/migrate/DomainMigrationRunnerTest.kt` | 导入/跳过/软失败 |
| `docs/rebuild/task-10-domain-cutover.md` | 本报告 |

未改：计划文件、个人包设备数据、CaptureKernel 核心路由逻辑（仅 Factory 薄挂钩）。

## 行为

1. **DI**：`AppContainer` 同时打开 `yuyue.db` 与 `yuyue-domain.db`。
2. **迁移门闩**：`domain` 四类计数均为 0 且 legacy 有数据 → `LegacyV7Importer` → `persistImport` → 校验 notebooks/entries/captures/media；不匹配则 `PrivacyLog` 计数详情并软失败，**不删除 legacy**。
3. **Dual-write**：`FEATURE_DOMAIN_WRITE=true` 时，`CaptureKernelFactory` 经 `DomainDualWrite` 镜像 inbox capture 与 materialized entries。
4. **不可变原话**：`Capture.rawText` 仅 `insertIgnore`；元数据走 `updateMeta`；`event_revisions` 为 `OnConflictStrategy.IGNORE`。

## 测试证据

```
.\gradlew.bat :android-app:testDebugUnitTest --offline --tests com.yuyue.app.domain.migrate.*
.\gradlew.bat :android-app:testDebugUnitTest --offline
```

覆盖：

| 用例 | 断言 |
|------|------|
| Gate | 空 domain + legacy 有数据 → Run；domain 非空 / legacy 空 → Skip |
| CountVerifier | 全匹配 ok；entries 不匹配仅计数文案；legacySourceCounts 与 importer 守恒 |
| Runner | empty domain←legacy 导入并核对；domain 非空不 persist；mismatch 软失败 |

## 协调说明（其他 agent）

- CaptureKernel / AppViewModel / shell：**优先用** `container.domainRepository` 与 `DomainDualWrite`；本任务已在 Factory 接好双写开关。
- 读路径仍走 legacy，切读需后续任务。

## 未决风险

1. 真机/QA 包尚未跑完整迁移演练签字。
2. Dual-write 失败仅记 `domain_dual_write_fail`（无正文）；legacy 仍成功，domain 可能短暂落后。
3. `mediaSha256ById` 仍为空时 sha=`unknown`（同 Task 3）。
4. Release 默认 `FEATURE_DOMAIN_WRITE=false`；迁移门闩仍会在空 domain 时导入。
5. Mutate / 修订恢复尚未双写 domain（仅新 capture/materialize）。

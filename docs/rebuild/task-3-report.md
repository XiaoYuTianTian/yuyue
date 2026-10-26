# Task 3 报告：领域库与 LegacyV7Importer

## 状态

**DONE_WITH_CONCERNS**

## 变更文件

| 文件 | 变更 |
|------|------|
| `android-app/.../domain/local/DomainEntities.kt` | Notebook / Capture / Event / EventRevision / MediaAsset / Topic·Person·Place + 关联 / UndoCommand / DomainIndexJob |
| `android-app/.../domain/local/DomainDaos.kt` | 领域 DAO（含修订追加、媒体引用计数查询） |
| `android-app/.../domain/local/DomainSchema.kt` | v1 CREATE SQL（JDBC 单测用，与 Room 实体对齐） |
| `android-app/.../domain/local/YuyueDomainDatabase.kt` | 独立 `yuyue-domain.db` Room DB **version 1** |
| `android-app/schemas/.../YuyueDomainDatabase/1.json` | KSP 导出 schema |
| `android-app/.../domain/migrate/LegacyV7Importer.kt` | 纯 Kotlin v7→domain 映射 |
| `android-app/.../data/BackupCryptoV2.kt` | YUY2 头（盐+KDF）编解码/加解密钩子；YUY1 不动 |
| `android-app/.../domain/migrate/LegacyV7ImporterTest.kt` | 导入单测 |
| `android-app/.../domain/local/DomainSchemaTest.kt` | schema/DB 名单测 |
| `android-app/.../data/BackupCryptoV2Test.kt` | YUY2 + YUY1 回归 |
| `android-app/.../ui/paper/PaperPrimitives.kt` | 修 Task2 引入的错误 `Modifier` import（否则全量编译失败） |
| `docs/rebuild/task-3-report.md` | 本报告 |

未改：`yuyue.db` / `YuyueDatabase` v7、主题色板（Task2）、个人包数据、计划文件。

## 测试

### RED（实现前）

新增测试引用尚未存在的 `LegacyV7Importer` / `DomainSchema` / `YuyueDomainDatabase` / `BackupCryptoV2`，预期无法编译。  
首次跑测时还被 Task2 半成品 `PaperPrimitives.kt`（错误 `import androidx.compose.ui.Modifier.Modifier`）挡住编译；已最小修复为项目惯用 `import androidx.compose.ui.Modifier`。

### GREEN（实现后）

```
.\gradlew.bat :android-app:testDebugUnitTest --offline --tests com.yuyue.app.domain.migrate.LegacyV7ImporterTest --tests com.yuyue.app.domain.local.DomainSchemaTest --tests com.yuyue.app.data.BackupCryptoV2Test
→ BUILD SUCCESSFUL
```

全量：

```
.\gradlew.bat :android-app:testDebugUnitTest --offline
→ BUILD SUCCESSFUL (exit 0)
```

覆盖点：空库→空 domain；条目数守恒；`rawTranscript`→不可变 `Capture.rawText`；媒体相对路径+sha+软删；未完成 ledger→inbox Capture；偏好快照 JSON；损坏修订/孤儿媒体安全跳过；YUY2 头盐/KDF；YUY1 往返仍可用。

## 未决风险

1. Domain DB 尚未接入 `AppContainer`/写路径；应用仍读写 v7（符合 Stage1 旁路）。
2. 未提供 `mediaSha256ById` 时 sha 记为 `unknown`；真机迁移需算真实 SHA-256。
3. `people` 仅在有明确分隔符时拆分；整串仍保留在 `legacyPeople`。
4. YUY2 仅为类型/单测钩子，Settings/`EncryptedBackupEngine` 仍走 YUY1+设备盐。
5. `DomainSchema` 手写 SQL 与 Room 导出需人工同步；改实体时两边都要更新。

# Task 1 报告：冻结契约与备份门禁

## 状态

**DONE**

## 变更文件

| 文件 | 变更 |
|------|------|
| `docs/rebuild/product-contract.md` | 新产品规格冻结 |
| `docs/rebuild/legacy-data-contract.md` | Room v7 / 备份 v3 契约 |
| `docs/rebuild/task-1-report.md` | 本报告 |
| `android-app/.../BackupImport.kt` | checksum 拒绝；`entryIdMap`；`mediaToUpsert` / `revisionsToUpsert`；RestoreAsCopy 媒体/修订重映射 |
| `android-app/.../BackupArchive.kt` | ZIP 路径穿越拒绝 |
| `android-app/.../YuyueRepository.kt` | `importArchive` 使用 `plan.mediaToUpsert` |
| `android-app/.../BackupV2Test.kt` | 强化 RestoreAsCopy 媒体断言 |

## 测试

### RED（修复前）

```
.\gradlew.bat :android-app:testDebugUnitTest --offline --tests BackupV2Test --tests BackupImportTest --tests BackupArchiveTest
```

- `BackupArchiveTest.rejectsArchiveEntryWithPathTraversal` 失败（未抛异常）
- 既有 `BackupV2Test` 对 `entryIdMap` / checksum throw / media remap 的期望在实现前不可编译或不通过

### GREEN（修复后）

```
.\gradlew.bat :android-app:testDebugUnitTest --offline --tests com.yuyue.app.data.BackupV2Test --tests com.yuyue.app.data.BackupImportTest --tests com.yuyue.app.data.BackupArchiveTest
→ BUILD SUCCESSFUL
```

全量：

```
.\gradlew.bat :android-app:testDebugUnitTest --offline
→ BUILD SUCCESSFUL (exit 0)
```

## 未决风险

1. `importJson` 仍不落 `revisionsToUpsert`；修订仅在 plan 层重映射，归档导入未持久化修订行。
2. RestoreAsCopy 后媒体文件名仍指向归档内原路径（正确），磁盘写入使用新 media id；需真机 ZIP 演练确认相册路径。
3. 加密备份盐仍设备绑定；跨设备恢复依赖后续 v4 盐进文件（Task 3/7）。
4. 无 git：无法用 worktree/commit 隔离；全部在工作区就地修改。

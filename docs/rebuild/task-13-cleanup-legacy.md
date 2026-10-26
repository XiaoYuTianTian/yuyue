# Task 13：遗留 Agent / 云路径清理

## 状态

**DONE**

## 目标

断开产品路径上可创建日记的 Agent 写工具；删除未接线的 Token / OfflineSync / Mutate UI；文档改为本机 CaptureKernel + YUY2，而非 cloud Agent。

## 变更

| 路径 | 说明 |
|------|------|
| `di/AppContainer.kt` | 不再构造 `AgentLoop` / `LocalAgentTools` / `TokenStore`；仓库只依赖 Room + 向量 |
| `data/YuyueRepository.kt` | 移除 `runIngestAgent` / `runRecallAgent` / `runMutateAgent` |
| `agent/AgentLoop.kt` | **删除** |
| `agent/LocalAgentTools.kt` | **删除**（含 upsert/mutate 写工具） |
| `agent/MiniMaxChatClient.kt` | 保留 chat；移除仅供 Agent 使用的 `toolDefinitions` |
| `data/TokenStore.kt` | **删除**（UI 未使用） |
| `data/OfflineSync.kt` | **删除**（no-op 遗留） |
| `ui/components/MutateSheet.kt` | **删除**（无引用） |
| `ui/AppViewModel.kt` / `MainActivity.kt` | 去掉 `TokenStore` 注入 |
| `README.md` | CaptureKernel / Domain / YUY2 |
| `docs/offline-decrypt.md` | YUY2 头格式 + YUY1 兼容说明 |
| `docs/testing/README.md` | 主路径描述对齐代码 |
| `docs/rebuild/legacy-data-contract.md` | `yuyue_secure` 标注已删除 |

未改：CaptureKernel、EncryptedBackupEngine、Domain dual-write、StructuredTurn / FastExtract 可选润色。

## 行为

1. **写入权威**：仅 CaptureKernel（及 Domain dual-write）创建/物化日记；可选润色不阻塞本机落库。
2. **无 Agent 写工具**：产品进程不再暴露 `upsert_entries` / `mutate_entries` tool-calling 循环。
3. **备份**：默认 YUY2；YUY1 仍可解密恢复。

## 测试

```
.\gradlew.bat :android-app:testDebugUnitTest --offline
```

## 关注点

- `server/` 与 `data/remote/YuyueApi` 仍为遗留对照，未本轮删除。
- `docs/待办进度.md` / 部分 `docs/superpowers/` 历史文案可能仍写 Local Agent；以本文件与 README 为准。
- 旧设备上可能残留空的 `yuyue_secure` SharedPreferences，无功能影响。

# Legacy 数据契约（Room v7 / 备份 v3）

> 冻结当前本机存储与备份格式。新领域库旁路迁移时必须可无损读取下列结构。  
> 依据：`YuyueDatabase.kt`、`Entities.kt`、`YuyueMigrations.kt`、`YuyueRepository.kt`、`BackupArchive.kt`。

## 数据库

| 项 | 值 |
|----|-----|
| 文件名 | `yuyue.db`（`YuyueDatabase`） |
| 版本 | **7**（`YuyueMigrations.LATEST`） |
| Schema 导出 | `android-app/schemas/.../YuyueDatabase/7.json` |
| 包名安全 | 个人包 `com.yuyue.app`；QA `com.yuyue.app.qa`。测试勿写个人包数据。 |

### 表（v7）

| 表 | 用途 | 关键字段 |
|----|------|----------|
| `notebooks` | 记事本/工作区 | id, name, slug, color, sortOrder, isDefault |
| `entries` | 日记条目 | id, notebookId, entryKind, status, title, body, rawTranscript, start/end/stated/updated/deletedAtMillis, done, utteranceId, place, people, diaryType |
| `entries_fts` | FTS4 | entryId, title, body, rawTranscript, place, people, diaryType |
| `entry_embeddings` | 本地向量 | entryId, model, dim, vectorBlob |
| `entry_revisions` | 修订快照 | id, entryId, snapshotJson, createdAtMillis, reason |
| `entry_media` | 媒体元数据 | id, entryId, requestId, mime, localPath, thumbPath, width, height, sortOrder, createdAtMillis |
| `capture_ledger` | 采集账本 | requestId, utteranceId, notebookId, rawTranscript, status, mediaJson, … |
| `pending_jobs` / `index_jobs` | 旧队列（过渡期保留） | — |

媒体二进制目录：`Context.filesDir/entry_media/`（`EntryMediaStore` / `AppContainer.mediaDir`）。

## 备份归档（exportVersion = 3）

### ZIP 布局

- `diary.json`：密封后的 `ExportResponse`
- `media/<id>.jpg`、`media/<id>_thumb.jpg`：与 JSON 中 `EntryMediaDto.fileName` 对齐
- 拒绝：`..` 路径穿越、绝对路径、缺 `diary.json` 的假 ZIP

### `ExportResponse` 字段

`exportVersion`（现为 3）、`schemaVersion`（现为 7）、`appVersionName/Code`、`exportedAt`、`userId`、`checksumSha256`、`notebooks`、`entries`、`revisions`、`media`。

校验：`BackupIntegrity.seal` / `verify`（SHA-256 over JSON with empty checksum）。损坏校验失败 → **禁止写入**。

加密包装：明文 ZIP 再经 `BackupCrypto` → `yuyue-backup-*.enc`；盐存在 `yuyue_backup_secure`。

## 导入策略

| 策略 | 行为 |
|------|------|
| `NewerWins` | 同 id 以更新时间较新者胜；旧入跳过 |
| `RestoreAsCopy` | 冲突条目换新 id、标题加「副本·」；**媒体/修订 entryId 重映射，并分配新主键**，避免粘到旧条目或覆盖旧媒体行 |

空库恢复：可建笔记本与条目；无条目时 `entriesToUpsert` 为空。  
预览：`EncryptedBackupInspect` 只读解密统计，不写库。

## SharedPreferences / 安全存储

| 名称 | 内容 |
|------|------|
| `yuyue_ui` | 主题等 UI |
| `yuyue_backup_secure` | 备份口令、盐、目录 URI、健康状态 |
| `yuyue_ai_secure` | AI base URL 等（密钥来自 BuildConfig） |
| `yuyue_profile_secure` | 显示名等资料 |
| `yuyue_secure` | ~~TokenStore JWT~~（已删除；旧安装可能仍残留空 prefs） |

## 兼容性承诺

1. 能读 v2 JSON（无媒体）与 v3 ZIP（含媒体）。
2. Room 迁移链 1→7 保持可运行；新 Domain DB 通过 `LegacyV7Importer` 旁路导入，不原地破坏 v7。
3. 个人包升级前必须：完整加密备份成功 + 空库/副本恢复演练通过 + 数量校验。

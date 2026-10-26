# Task 7 报告：成熟产品功能（首批）

## 状态

**DONE_WITH_CONCERNS**

## 目标（plan §7 第一批）

桌面小组件/快捷采集、文本+图片分享、应用锁、可靠加密备份（YUY2）、恢复预览、废纸篓永久删除、持久撤销、主题选择、Markdown/PDF/TXT 导出、那年今日本地通知；Settings 默认加密备份，明文 ZIP 标「未加密导出」；Auto Backup 只含已验证加密归档。

未做：第二批人物/地点时间线（Task 8）。

## 变更文件（主要）

| 文件 | 变更 |
|------|------|
| `ui/CaptureLaunch.kt` + `CaptureLaunchTest` | 快捷入口判定打开语音球 |
| `widget/QuickCaptureWidget.kt` + layout/xml | 桌面小组件 → `QUICK_CAPTURE` |
| `ui/SharedMediaIntake.kt` + Test | 文本/图片分享决策 |
| `MainActivity.kt` / Manifest | 图片 SEND、语音快捷、锁门、加密默认导出 |
| `security/AppLockPolicy.kt` + `AppLockStore.kt` + Test | PIN 哈希/宽限期；偏好位留给生物识别 |
| `data/EncryptedBackupEngine.kt` / `EncryptedBackupInspect.kt` | **写出/预览 YUY2**（头内盐）；YUY1 仍可解密 |
| `data/EncryptedBackupWork.kt` / `YuyueApplication` | 无目录时写 `filesDir/encrypted-backups/`；有密钥才 enqueue |
| `res/xml/full_backup_content.xml` 等 | 含 `encrypted-backups/`；排除 lock/backup/ai secure |
| `data/TrashPurge.kt` + `YuyueRepository.purge*` | 永久删除（媒体/修订/FTS/向量） |
| `ui/TrashScreen.kt` | 单条/清空永久删除确认 |
| `ui/PersistentUndoCodec.kt` + `PersistentUndoStore.kt` | 进程级持久撤销（legacy prefs；Domain UndoCommand 仍旁路） |
| `ui/shell/screens/VaultPaperScreen.kt` / Settings | 锁、密钥、加密默认、未加密导出、MD/TXT/PDF、主题 |
| `data/DiaryExport.kt` + Test | Markdown / TXT / 简易 PDF |
| `notify/OnThisDayNotify.kt` + `OnThisDayWork.kt` | 本地通知内容 + 每日 09:00（上海）调度 |
| `docs/rebuild/task-7-report.md` | 本报告 |

未改：计划文件、个人包数据、Task 8 差异化。

## 各需求结论

| # | 项 | 结果 |
|---|----|------|
| 1 | 桌面小组件 / 快捷采集 | **修**：快捷/组件走 `requestVoiceCapture`（语音球），不再开键盘 |
| 2 | 文本+图片分享 | **验+补**：文本路径保留；新增 `image/*` + `SharedMediaIntake` |
| 3 | 应用锁 | **补**：PIN + 30s 宽限期；生物识别 **偏好已存，Prompt 未接线**（见 concerns） |
| 4 | 加密备份 + 预览 | **补**：Engine 默认 YUY2；Inspect 按格式解密；本地 auto-backup 目录 |
| 5 | 废纸篓永久删除 | **补**：`purgeEntry` / `purgeTrash` + UI |
| 6 | 持久撤销 | **补**：`PersistentUndoStore`（legacy）；Domain `UndoCommand` 表仍未接写路径 |
| 7 | 主题 | **已有**：`ThemeStore` + Vault/MainActivity 明暗芯片 |
| 8 | MD/PDF/TXT | **补**：Vault/Settings 可读导出；PDF 为拉丁回退简易 PDF |
| 9 | 那年今日通知 | **补**：WorkManager + 通知渠道；需运行时通知权限 |

## 测试

### RED → GREEN（实现前引用未存在类型，编译失败）

### 聚焦（实现后）

```
.\gradlew.bat :android-app:testDebugUnitTest --offline --tests com.yuyue.app.ui.SharedMediaIntakeTest --tests com.yuyue.app.security.AppLockPolicyTest --tests com.yuyue.app.ui.PersistentUndoCodecTest --tests com.yuyue.app.data.DiaryExportTest --tests com.yuyue.app.notify.OnThisDayNotifyTest --tests com.yuyue.app.notify.OnThisDayWorkTest --tests com.yuyue.app.data.TrashPurgeTest --tests com.yuyue.app.ui.CaptureLaunchTest --tests com.yuyue.app.data.EncryptedBackupInspectTest --tests com.yuyue.app.data.BackupCryptoV2Test
→ BUILD SUCCESSFUL
```

### 全量

```
.\gradlew.bat :android-app:testDebugUnitTest --offline
→ BUILD SUCCESSFUL (exit 0)
```

## 未决风险 / Concerns

1. **生物识别 Prompt**：为保持 `--offline` 可构建，未引入未缓存的 `androidx.biometric`；Vault 可存偏好，真机需加依赖并接线 `BiometricPrompt`（或联网拉包一次）。
2. **POST_NOTIFICATIONS / 小组件钉桌面**：Android 13+ 通知权限与 OEM 桌面钉小组件需真机确认；Play Console 与厂商白名单不在本任务范围。
3. **PDF 中文**：简易 PDF 用 Helvetica + ASCII 回退；完整中文 PDF 需真机 `PdfDocument` 或嵌入字体。
4. **YUY1 → YUY2**：新备份为 YUY2；旧 YUY1 仍可恢复（需本机盐）。跨设备恢复应使用 YUY2。
5. **Auto Backup**：规则只 include `encrypted-backups/`；是否被云备份真正带走取决于设备备份开关与 Google/OEM 策略。
6. **Domain UndoCommand**：表已存在（Task 3），本任务用 legacy prefs 持久化，待 Domain 写路径接通后可迁移。

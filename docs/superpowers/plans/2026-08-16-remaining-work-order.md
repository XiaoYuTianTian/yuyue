# 未做能力实施顺序

原则：先保证「记进去、排好序、能问回来」可信；再扩大单次录入量；最后才做更重的隐私和多设备。

## P0 — 真机跑通（先做，不改产品逻辑）

本机命令行编不过，是因为 Android SDK 路径失效。在 Android Studio 打开项目，写好 `local.properties` 的 `sdk.dir`，运行 `android-app`。

验收：无 server 能进首页；一段话拆多条上日历；回忆「今天/去年某天」按时间列出；断网日历可读，写入/回忆明确报错。

## P1 — 写入前预览确认

`upsert_entries` 先出预览，不写库；`IngestPreviewSheet` 勾选确认后再 `EntryDao.upsert` + 向量索引。

主要文件：`LocalAgentTools.kt`、`AppViewModel.kt`、`IngestPreviewSheet.kt`、`HomeScreen.kt`。

## P2 — 长语音分段

`SpeechTranscriber` 自动续听，分段拼进同一编辑框，用户提交才给 Agent。不接云端 ASR。

## P3 — 端侧 Embedding

`LocalVectorStore` 换成本地模型；对话仍走 MiniMax。APK 更大，后做。

## P4 — 加密备份 / 多设备

先加密导出+导入；再考虑可选同步。最后做。

## 本期不做

云端 ASR、短信登录主路径、记事明文回存 Ktor/Postgres。

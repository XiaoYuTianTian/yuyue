# 语曰质量测试报告 · 2026-09-19

run_id: `e2e-20260919`  
设备: Xiaomi MIX 3 `45c5974e` Android 10 / API 29 / arm64-v8a  
包: `com.yuyue.app.qa` `1.0.15+20260919.1343-qa` (userId=10194)  
个人包: `com.yuyue.app` `1.0.15+20260826.2251` (userId=10250) **未改动**

权威账本: `docs/testing/results.jsonl`（同一 case_id 以最后一行为准）  
缺陷: `docs/testing/defects.md`  
证据: `docs/testing/evidence/device/e2e-20260919/`、`docs/testing/evidence/ai-smoke-20260919.jsonl`

## 总览（去重后）

| 状态 | 数量 | case_id |
|------|------|---------|
| PASS | 13 | ENV-001/002/003, JVM-001, JVM-WORKER-001, MD-CAPTURE-001/002/004, MD-MUTATE-001, MD-AI-003/004, QC-UX-002, QC-PRIV-001 |
| FAIL | 8 | FI-ENTRY-001/002, FI-PERM-001, QC-A11Y-001, AI-SMOKE-001, QC-UX-001, MD-AI-001(快模型), MD-AI-002(模型层) |
| BLOCKED | 3 | MD-CAPTURE-003(系统相册), MD-RECALL-001(adb 中文), AI-GOLD-001(无 40 条金标) |
| UNSUPPORTED | 1 | QC-BACKUP-002 |
| 部分 | 1 | QC-BACKUP-001 导出已验证；清空再导入/错文件未做 |

false-pass：本轮把 MD-CAPTURE-003 误标 PASS 已纠正为 BLOCKED（相册对话框未真正打开）。

## 真机主路径

- 首启教学页 →「开始使用」→ 欢迎页空态「记下你的第一篇日记」。
- 文字发送 `drank drank`：250ms 截图已有「已保存」+ 队列「正在提取」；此时 Feed 仍空、Room entries=0。约 9s 后 Room 1 条、Feed 出现。**完成前无假标题。**
- FastExtract 把英文 drank 写成标题「喝酒」、正文「今天喝酒了。」原话仍在详情「原话」。见 DEF-009。
- 飞行模式 `offline cofee now` 原话落盘。
- 杀进程：队列恢复「正在处理」；`kill test now` 未重复落盘，后变为「需要你确认」。
- 长按删除需确认 → `deletedAtMillis` 软删 → 废纸篓可恢复。Toast「撤销」约 2s 消失（DEF-010）。
- 欢迎 / 月历 19 号有记事 / 日轴 1 则 / 详情 14:37 一致。状态栏「上午 6:xx」与条目 14:37（Asia/Shanghai）不一致，DEF-006。
- 查找时无语音球。
- 备份：`yuyue-backup-20260919-145520-290.zip` 含 `diary.json` exportVersion=3、2 本、2 条。
- 按住语音球出现「正在准备识别…」，松手空识别回到「按住说一句」，无错误提示（QC-UX-001 FAIL）。
- `ACTION_SEND` 热启动不写库；冷启动 8s 仍 0 行（记事本未就绪）。
- `QUICK_CAPTURE` 只打开欢迎页。

## AI

| 模型 | 结果 |
|------|------|
| SiliconFlow Qwen2.5-7B | 12/12 HTTP 402，不可用 |
| MiniMax | 9/12。失败：s04/s08 JSON 解析 got=None；s06 注入句 got=clarify |
| LocalRouter JVM | 回忆/修改关键词分类 4/0 |
| DefaultStartAt JVM | 未说时刻不落 00:00，4/0 |
| 40×3 金标 | 语料集不存在，未跑 |

## 未关闭缺口

1. 系统相册/SAF 选择器 adb 点不准 → 离线照片、备份导入/错文件。
2. 小米 clipboard 拒绝 shell 写入 → 中文回忆/拆条真机未注入。
3. SiliconFlow 402 导致线上 FastExtract 全灭，真机仍可能打到付费失败。

## 修复落地（同日晚 · 代码 + QA 复测）

QA 包已重装 `com.yuyue.app.qa`，个人包未动。

| 项 | 结果 | 证据 |
|----|------|------|
| FI-ENTRY-002 分享冷/热启动 | PASS | `share-hot-db.json` 冷 `…cold-task1`、热 `…hot-task1` 均为 Completed |
| FI-ENTRY-001 QUICK_CAPTURE | PASS | `fix-quick.png` 出现「要记的事…」+ 键盘 |
| QC-UX-001 空语音 | PASS | `fix-voice-empty.png`「没听清，请再按住说一次」 |
| QC-A11Y-001 | PASS | `fix-voice-empty-nodes.json` `content-desc=按住说话` |
| FastExtract 4xx | JVM PASS | `FastExtractFailureTest`；402 账单仍 OPEN |
| 多事件拆条 | JVM PASS | `FastExtractParserTest.commaSeparatedEventsBecomeTwoEntries` |
| Unix `gradlew` | FIXED | 仓库根目录已有 `gradlew` |
| FI-PERM-001 首启麦权限 | INCONCLUSIVE | 代码已不在 `onCreate` 申请；未清 QA 数据看系统对话框 |

未关：SiliconFlow 402、相册选择器、中文剪贴板、40×3 金标（现仅有 `gold40.jsonl` 占位 3 条）、英文 drank→喝酒。


# 异步整理任务（Outbox）设计

日期：2026-08-22  
状态：已与产品确认方向，待实现计划  
关联：现有 `CaptureLedger`、`CaptureQueueBar`、`CaptureQueueWork`

## 目标（用户核心诉求）

用户说完一段话后：

1. **不必占满当前页、不必一直转圈等待**
2. 可立即继续浏览、搜索、切换工作区、再记一条
3. **能看见**每条原话的整理状态（等待 / 整理中 / 暂停 / 失败重试 / 需确认）
4. **能管理**：暂停、继续、丢弃、重试；暂停后可改原话
5. **整理完成后**应用内通知（可点跳转到结果日记或回忆/修改结果）
6. **不在主日记列表里**用本机猜测的日期/标题占位，避免「先出现在今天、后又移到下周三」的信任问题

## 非目标（v1）

- 本机小模型 / 端侧 LLM 结构化
- 未确认时间写入 Feed 的「假日记」或草稿行
- 列表顶部「整理中」横幅（用户选 **A**：仅底部条 + 详情）
- Android 系统级通知栏（可 v2）
- 整理中允许编辑（仅 **暂停后** 可改原话）

---

## 产品决策摘要

| 项 | 决定 |
|----|------|
| 任务展示 | **仅**底部 `CaptureQueueBar` + 详情抽屉；不占首页/日时间线列表 |
| Feed 写入时机 | **大模型（或确定性本地 mutate）确认后**才写入/更新正式 `Entry` |
| 本机快车道 | **不**向 Feed 预写；任务卡片只展示原话 + 状态文案 |
| 明显查/改 | `LocalRouter` 判为 Recall/Mutate 时**不创建**「待整理」假记录；走现有前台或队列逻辑 |
| 非明显查/改 | 原话入账本 → 后台整理；完成前只在 Outbox 可见 |
| 暂停语义 | 取消进行中的 API/重试；标记 Paused；用户可改原话 / 丢弃 / 继续 |
| 整理中编辑 | **禁止**；仅 Paused / ForegroundRequired 可改 |

---

## 架构：两层数据

```text
CaptureLedger（已有）
  requestId, rawTranscript, notebookId, status, lastError, …

Entry（正式日记）
  仅 Completed / 确定性 Mutate 成功后写入
```

**Outbox UI** = `CaptureLedger` 活跃状态的呈现，不新增「staging Entry」表。

### 状态机（账本 / 任务）

```text
Pending      → 等待整理（入队）
Processing   → 正在调模型 / 本地处理
Retryable    → 失败或限流，自动或手动重试
ForegroundRequired → 需用户补充（澄清 / 选改哪条）
Paused       → 用户暂停（新增）
Completed    → 成功结束（从活跃队列消失）
Discarded    → 用户丢弃（等同 Completed，无日记产出）
```

`Paused` 实现：在 `CaptureStatus` 增加枚举，或 `ForegroundRequired` + `lastError="已暂停"`（实现时选一种，UI 统一）。

### 用户操作矩阵

| 状态 | 暂停 | 继续 | 丢弃 | 重试 | 改原话 | 点通知跳转 |
|------|------|------|------|------|--------|------------|
| Pending/Processing | ✓ | — | ✓ | — | — | — |
| Retryable | ✓ | — | ✓ | ✓ | — | — |
| ForegroundRequired | ✓ | ✓（继续澄清） | ✓ | — | ✓（暂停后） | — |
| Paused | — | ✓ | ✓ | — | ✓ | — |
| Completed | — | — | — | — | — | ✓ |

---

## 交互流程

### 1. 提交（不阻塞页面）

1. 用户发送语音/文字
2. **立即**：保存原话到 `CaptureLedger`；底部出现/更新 `CaptureQueueBar`
3. **立即**：结束整页 `jobRunning` 阻塞（或仅极短「已保存」态）；收起语音稿区（已有 `voiceNonce` 等）
4. **后台**：`CaptureQueueWork` / 前台协程调用 `StructuredTurn`
5. 用户可自由导航

### 2. 整理成功

| 云端意图 | 行为 | 通知 |
|----------|------|------|
| Record | `saveStructuredEntries` → 刷新 Feed | Toast「已记下：{title}」**可点击** → `EntryDetail` |
| Recall | 现有回忆卡 / 打开某日 | Toast + 可选跳转 |
| Mutate | 改/删目标条 | Toast「已改/已移入废纸篓」+ 跳转目标 |
| Clarify | `ForegroundRequired`；底部追问 | 任务详情「需要你补充」 |

### 3. 失败 / 限流

- 状态 `Retryable`；队列条摘要「等待重试」
- 详情：重试、暂停、丢弃
- **不**写入 Feed

### 4. 暂停

- 取消当前 Job（`WorkManager` cancel + 协程 cancel）
- 状态 → `Paused`
- 详情展示可编辑原话；保存后仍 Paused，点「继续」用新原文重新整理

### 5. 丢弃

- `captureLedger.discard(requestId)`（已有）
- 若有关联 `pendingCaptureRequestId` 澄清态一并清理
- 从队列消失；**从未**写入 Feed

---

## UI：方案 A

### 底部 `CaptureQueueBar`（全主屏共有）

- 有活跃任务时显示（含搜索模式，已实现）
- 摘要：「1 条正在整理」/「2 条等待重试」/「需要你确认」
- 副标题：最近一条原话截断
- 点击 → **详情抽屉/对话框**（已有 LazyColumn，需增强）

### 详情抽屉（增强）

每条任务：

- 状态标签 + 时间
- **完整原话**（Paused 时可 `OutlinedTextField` 编辑 + 保存）
- 操作按钮（按状态显示）：暂停 / 继续整理 / 重试 / 丢弃
- ForegroundRequired：说明 +「去补充」→ 关闭详情，聚焦底部输入澄清

### 不做的展示

- 首页 Feed、日时间线、月历 **不**插入「整理中」行
- 无列表顶横幅

---

## 与本机抽取的关系

**v1：不做 Feed 预写，不做本机日期写入 Entry。**

可选（v2，非 v1）：

- 任务详情内灰色 hint「可能提到：下周三」（只读、标注待确认），**不**驱动 `startAtMillis`
- 仅当规则置信度极高（单独 spec）才讨论预填

理由：用户明确反馈错误日期比等待更伤体验。

`LocalRouter` 仍用于：

- 明显 Mutate/Recall → 跳过「先当记录整理」路径（与范围 B 一致）
- 离线时 `processCaptureInBackground` 行为保持

---

## 技术改造要点（实现时参考）

| 区域 | 改动 |
|------|------|
| `AppViewModel.submitStructuredTurn` | 保存原话后尽快 `_jobRunning = false`；长耗时放后台；澄清态例外 |
| `CaptureQueueWork` | 支持 Paused 不自动 pickup；cancel tag per requestId |
| `CaptureLedgerService` | `markPaused`, `updateRawTranscript`, 完成时 `CaptureEvents` 带 `entryId` 供跳转 |
| `CaptureEvents` | 扩展：`Completed(entryId?, action: Record/Delete/…)` |
| `ObserveYuyueToasts` / Toast | 支持 `toastAction` 跳转 `viewingEntry` |
| `CaptureQueueBar` | 暂停/继续/编辑原文（Paused） |
| `MainActivity` | Toast 点击 → `viewingEntry` 或 `openDay` |

### 验收标准

- [ ] 发送后 1s 内可滚动首页、可打开设置，底部条可见任务
- [ ] 整理 10s+ 期间 Feed **无**新增占位行
- [ ] 完成后 toast 可点开对应日记
- [ ] 暂停后可改正文；继续后用新正文整理
- [ ] 丢弃后队列与 Feed 均无该条
- [ ] 限流任务可重试；失败不丢原话
- [ ] 搜索时仍能看到底部队列条

---

## 与历史方案关系

- 取代「草稿态 B：灰字进 Feed」方向——**不再采用**
- 保留并加强现有队列与账本，而非另建假消息层
- 首次欢迎页、工作区批量、状态点等已交付功能不受影响

## 实现顺序建议

1. 提交路径去阻塞 + 完成事件带 `entryId` + Toast 跳转  
2. 队列详情：暂停 / 继续 / 改原文（Paused）  
3. `CaptureStatus.Paused` + WorkManager cancel  
4. 回归：澄清、回忆、修改、限流、搜索底栏  

# 测试契约

## 产品冻结

以仓库代码为准，不是 `README.md` 里的 MiniMax tool-calling 描述。

```
用户输入（语音 Vosk / 文字 / 照片 / ACTION_SEND）
  → CaptureLedger.saveCapture
  → 无 Key 或离线：LocalRouter + LocalRecordFallback（record 可落盘；recall/mutate 等网络）
  → 有网：FastExtractClient（默认 Qwen2.5-7B，8s）
       · intent=record → 立即 Room 落盘（id = requestId:idx）→ MiniMax polish（禁止增删条）
       · intent=recall/mutate → 前台 StructuredTurn
  → StructuredTurnClient MiniMax JSON（失败再要一次合法 JSON）
  → Sufficiency / DefaultStartAt / DonePolicy / PolishDatePolicy
  → recall：Repository.recall + RecallUnlock/Mixer/Grounding
  → mutate：DeterministicMutate，多候选让用户选
  → 向量：本机 BGE，失败不挡写入
```

Oracle 优先级：Room 状态 > API 原始响应 > UI 文案 > 截图观感 > 模型口述。

## 状态机

每个 `case_id` 只能停在：

- `PLANNED` → `READY` → `EXECUTED` → `VERIFIED` → `PASS` | `FAIL`
- 旁路：`BLOCKED`（缺条件）、`UNSUPPORTED`（能力不够）、`INCONCLUSIVE`（证据冲突）、`WAIVED`（人工豁免，不算通过）

## MFQ

- **MD** 单功能：首启、采集、账本、意图、欢迎/月历/日轴、回忆修改、CRUD、备份、检索
- **FI** 交互：权限时序、队列中再说话、杀进程、查找与 Dock 互斥、返回栈、分享/快捷方式
- **QC** 质量：功能、体验、稳定、性能、网络、权限、兼容、隐私、安全、可访问性、视觉

## PPDCS

对每个 MD 展开 Parameter / Partition / Data / Combination / State。  
弱模型必须覆盖：非法 JSON、截断、错误枚举、recall→record 误判、多事件无法拆条。

## 对标微信 / X（可量化，不抄像素）

| 指标 | 门槛 |
|------|------|
| 提交后首个反馈 | ≤ 300ms 出现队列/toast |
| 触控目标 | ≥ 48dp 或有无障碍替代 |
| 核心记下动作数 | 记录现状；微信松手即发作为对比项，不强制改产品 |
| 失败可恢复 | 必须有重试/撤销/丢弃之一 |
| 草稿保持 | 切查找再回来不得丢未发送文稿（若当前实现会丢，记 FAIL 而非改 oracle） |

## 红线

1. false-pass：产品有缺陷却报 PASS
2. 错误写库：把 recall/mutate 当成 record 新增日记
3. 证据缺失仍报通过
4. healer 修改断言或期望值
5. 把 BLOCKED 记成 FAIL 或 PASS

# 语曰 · P0+P1 结构化日记重构设计

日期：2026-08-17  
状态：待你确认后按实现计划落地  
范围：**P0（日时间线新页）+ P1（一次结构化 LLM 调用 + 自动记录）**  
明确不做（P2/P3）：完整检索结果页、多轮追问 UX、MiniMax TTS、本机 embedding

> 本文覆盖并修订 `2026-08-16-yuyue-timeline-design.md` 中与本阶段冲突的口径：  
> - 记录**不再预览确认**，解析成功即入库  
> - `done` **按相对当前时间硬规则**写入（见 §4）  
> - 某天浏览改为**独立页面**，不再用 BottomSheet

---

## 1. 产品一句话（本阶段）

用户点进某一天，用时间线浏览当天日记（只看标题）；点「记录」后，一次大模型调用整理结构化字段并自动落库，用标题弹窗确认；检索与多轮对话下期再做。

---

## 2. 已锁定产品决策

| 项 | 决定 |
|----|------|
| 日浏览 | 点日历日期 → **新页面**时间线，不是弹窗 |
| 时间线密度 | 每条先显示 **LLM 总结标题**；点入再看详情 |
| 记录按钮 | 一次 LLM 调用：意图 + 标题 + 时间 + 地点 + 人物 + 类型 + 整理正文 |
| 返回格式 | **严格 JSON**（见 §5），本地解析；解析失败可重试/提示 |
| 记录意图 | **自动入库**，弹窗展示标题，**约 3 秒**关闭 |
| 检索意图（P1） | 识别后结果卡提示「检索下期上线」；暂不二次 LLM 筛选（P2） |
| 信息不足 | Schema 预留 `intent=clarify`；P1 仅结果卡展示追问文案，不做完整多轮会话态 |
| 反馈形态 | **结果卡 C**（底栏上方），不做完整聊天历史 |
| 完成态 | `start_at ≤ now` → 已完成（绿）；`start_at > now` → 未完成（红） |
| 手动改红绿 | 详情页允许覆盖；**不做**后台按时间批量改历史 |
| 听写 | 继续本机 **Vosk** |
| 播报 | MiniMax **TTS** 放 P3；同 Key 可用 |
| Embedding | MiniMax 无可靠产品；P1 用 FTS；P3 再考虑本机 ONNX（中文小模型） |
| Mutate/删改 | P1 不走语音意图；详情页按钮删/改 |

---

## 3. 信息架构与导航

```text
Home（月历）
  ├─ tap day → DayTimelineScreen（全日按 startAt 升序）
  │              └─ tap row → EntryDetailScreen（标题/正文/时间/地点/人物/类型/done）
  ├─ 底部 VoiceOrb + 草稿卡 + 结果卡
  └─ Settings / Trash（现有 overlay 保留）
```

**DayTimelineScreen**

- 顶栏：返回 +「M月D日 · 周X」
- 主体：纵向时间线；仅渲染有记录的条目（不铺空 24 小时）
- 行内容：左侧时刻、中间标题（单行省略）、右侧绿/红点
- 空态：「这一天还没有日记」+ 引导按住说话
- 动效：进入共享元素/列表 stagger fade（轻量，避免炫技）
- 从时间线返回月历时，该日保持 `focusedDay` 高亮

**EntryDetailScreen**

- 展示结构化字段；可编辑标题/正文/done；删除进废纸篓
- 地点、人物、类型只读或可编辑（P1：可编辑文本即可）

**废弃**

- `DayDetailSheet` 作为主路径；实现后删除调用，文件可移入废弃或删除

---

## 4. `done` 硬规则

写入时（及模型未给 `done` 时）：

```text
done = (startAtMillis <= System.currentTimeMillis())
```

- 「今天下午 3 点」在 15:00 之前写入 → 未完成（红）  
- 同一条在 15:00 之后**不会**自动变绿；用户可在详情手动改  
- 日历月点：当天任一条 `done=false` → 红点，否则绿点（保持现逻辑）

模型输出里的 `done` 字段：**忽略**，一律本机按上式计算，避免和产品口径打架。

---

## 5. 一次结构化 LLM 调用

### 5.1 请求

系统提示 + 用户消息包含：

- 设备当前时间 ISO-8601（`Asia/Shanghai`）
- 当前记事本 id/name
- 可选：粗定位（经纬度或逆地理短地址；无权限则为空，模型可留空 `place`）
- 用户原话（草稿全文）

**禁止**本阶段再走多步 `upsert_entries` tool-loop 做记录。记录 = 一次 chat completion → 解析 JSON → Room upsert。

### 5.2 响应 JSON Schema（唯一合法形态）

模型必须只输出一个 JSON 对象（不要 markdown 围栏；若偶发带围栏，解析器剥掉）：

```json
{
  "intent": "record | recall | clarify",
  "message": "给用户看的一句话（可空）",
  "clarify_question": "仅 intent=clarify 时必填",
  "entries": [
    {
      "title": "短标题，≤20字",
      "body": "整理后的日记正文",
      "start_at": "2026-08-17T15:30:00+08:00",
      "place": "",
      "people": [],
      "diary_type": "life | work | meal | meeting | travel | mood | other"
    }
  ],
  "recall_query": {
    "time_hint": "",
    "type_hint": "",
    "title_hint": "",
    "keyword": ""
  }
}
```

规则：

| intent | entries | 客户端行为（P1） |
|--------|---------|------------------|
| `record` | 1..N | 逐条入库；弹窗显示**第一条标题**（多条则「标题 等 N 条」）；3s 关；刷新月点；若正在看当天时间线则列表刷新 |
| `recall` | 可空 | 结果卡：`message` 或固定文案「检索能力下期开放」；**不写库** |
| `clarify` | 可空 | 结果卡展示 `clarify_question`；草稿保留，用户可追加后再点记录 |

缺省：

- `start_at` 缺失/非法 → 当前时间  
- `place` 空 → 若有定位则填定位短串，否则 `""`  
- `people` 缺失 → `[]`  
- `diary_type` 非法 → `other`  
- `title` 空 → 截取 `body` 前 12 字  

### 5.3 本地解析

- `StructuredDiaryParser`：字符串 → `StructuredDiaryResult`  
- 失败：结果卡错误 + 保留草稿；不计为成功记录  

### 5.4 与旧 Agent 关系

- `runIngestAgent` / tool upsert **退出记录主路径**  
- 回忆 Agent、Mutate Agent：P1 可保留代码但语音主按钮不再路由到它们  
- 删除 `InputIntents` 关键词路由（记录按钮只走结构化调用）

---

## 6. 数据模型变更

`EntryEntity` 增列（Room migration）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `place` | `String?` | 地点 |
| `peopleJson` | `String?` | JSON 数组字符串 |
| `diaryType` | `String` | 默认 `other` |

现有 `title` / `body` / `rawTranscript` / `startAtMillis` / `done` / `utteranceId` 保留。  
同一次「记录」按钮共享一个 `utteranceId`。

---

## 7. 记录交互状态机（P1）

```text
idle → drafting（草稿可见）→ submitting（按钮「保存中…」，禁重复点）
        → success_toast（3s 标题弹窗）→ idle（清草稿）
        → result_card（clarify / recall / error）→ drafting（保留或清草稿视 intent）
```

- 提交中：禁日历切换、禁二次提交（单一 `jobRunning`）  
- 成功弹窗期间：可点外部关闭；3s 到自动关  
- 进度：**仅草稿卡一条**进度，不要全屏第二进度条  

---

## 8. UX / 动效原则（你在意好看）

- 时间线：清晰竖轴 + 时刻对齐，避免卡片堆叠过多阴影  
- 行点击：ripple + 轻量共享元素到详情标题  
- 成功弹窗：居中毛玻璃/暗色面板，标题大字，副文「已记下」  
- 结果卡：与草稿卡同一视觉家族（圆角 16、surfaceContainer）  
- **禁止**：默认紫色渐变、过量 glow、首屏信息过载  
- 验证：Android 模拟器或真机；改 UI 后 `assembleDebug` + 推 APK；关键页可用 Compose Preview  

---

## 9. 语音与检索（调研结论写入规格）

| 能力 | P0/P1 | 后续 |
|------|-------|------|
| ASR | Vosk 本机 | — |
| TTS | 不做 | P3 MiniMax `/v1/t2a_v2` 同 Key |
| 检索 UX | 不做 | P2 结果页 + 二次整理 |
| Embedding | 不做 | P3 本机 ONNX 中文小模型；一期 FTS 足够 |

---

## 10. 成功标准（验收）

1. 点某日进入**全屏时间线**，日记按时间排序，只显示标题。  
2. 点标题进入详情，可见正文与结构化字段。  
3. 说「今天下午喝了咖啡」→ 点记录 → JSON 解析 → 自动入库 → **约 3 秒标题弹窗** → 当天时间线出现新标题。  
4. 未来时间的事显示红点/未完成；过去/当前及以前显示绿/已完成。  
5. 问句被判为 `recall` 时不写库，结果卡有说明。  
6. 无双进度条、无预览确认页、无关键词意图误路由。  

---

## 11. 风险与治理

| 风险 | 对策 |
|------|------|
| 模型输出非 JSON | 强约束 prompt + 剥围栏 + 失败可重试 |
| 一次调用字段多、易漏 | Schema 少而硬；缺省规则本机补全 |
| 定位权限拒 | `place=""`，不阻塞记录 |
| Room 升级 | 显式 Migration，保留旧数据 |
| 旧预览/Agent 死代码 | P1 落地后删除主路径引用 |
| UX 难验 | 每完成时间线/弹窗任务即装真机看 |

---

## 12. 文件边界（实现时）

| 单元 | 职责 |
|------|------|
| `DayTimelineScreen` | 日时间线 UI |
| `EntryDetailScreen` | 详情（可基于现有 Edit 改造） |
| `StructuredDiaryClient` | 一次 chat + 原始字符串 |
| `StructuredDiaryParser` | JSON → 领域对象 |
| `DoneRules` | `doneFromStartAt(start, now)` |
| `AppViewModel.submitStructured` | 替代 `submitIngest` 主路径 |
| `ResultCard` / `SavedTitleDialog` | 结果卡与 3s 弹窗 |
| Room Migration N→N+1 | 新列 |

---

确认本规格后，按 `docs/superpowers/plans/2026-08-17-p0-p1-structured-diary.md` 逐步实现。

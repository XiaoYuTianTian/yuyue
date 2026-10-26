# 缺陷账本

只收录有复现步骤与证据的项。状态：`OPEN` | `CONFIRMED` | `FIXED` | `WAIVED`。

## 静态代码确认（待设备复测）

| id | 严重度 | 现象 | 证据 | 状态 |
|----|--------|------|------|------|
| DEF-001 | P1 | `QUICK_CAPTURE` 在 Manifest/shortcuts 声明，MainActivity 未处理 | 已修：`CaptureLaunch` + `requestComposerFocus`。复测 `fix-quick.png` 出现「要记的事…」 | FIXED |
| DEF-002 | P1 | 首次教学页出现前 `onCreate` 即申请麦克风 | 已删 `onCreate` 申请；Dock 按住时再申请。未 `pm clear` 复测首启系统对话框 | CONFIRMED（代码已改） |
| DEF-003 | P1 | `ACTION_SEND` 热启动丢；冷启动 notebooks 未就绪直接失败 | `SharedTextIntake` + `onNewIntent` + `singleTop`。`share-hot-db.json` 冷/热各 Completed | FIXED |
| DEF-004 | P0 | FastExtract 未知 intent 强制 record；polishLock 禁止拆条 | 回忆/修改关键词不再落盘；`MultiEventSplit`：「明早买菜，下午跑步」两条 start_at（JVM） | FIXED（JVM；真机拆条未再跑） |
| DEF-005 | P2 | 加密备份引擎未接到设置页；设置导出为明文 ZIP | 本计划不修；设置页无「加密」字样 | WAIVED |
| DEF-006 | P2 | 时区写死 Asia/Shanghai | 本计划不改时钟语义 | WAIVED |
| DEF-007 | P2 | Debug/QA APK 把 LLM Key 编进 BuildConfig | `release` 的 `DEFAULT_LLM_API_KEY` / `SILICONFLOW_API_KEY` 已强制 `""`；debug/qa 仍可有密钥 | CONFIRMED（release 已空） |
| DEF-008 | P1 | Gradle Wrapper 缺 Unix `gradlew` | `gradlew` 已生成；`gradle-wrapper.jar` 43504 bytes | FIXED |
| DEF-009 | P1 | FastExtract 把英文 `drank drank` 落成标题「喝酒」 | `18-9s-db.json`、详情页原话仍保留 | CONFIRMED |
| DEF-010 | P2 | 废纸篓撤销 toast 约 2s 内消失 | `ObserveYuyueToasts` 有 action 时改 `SnackbarDuration.Long`（约 10s）；未再真机计时 | CONFIRMED（代码已改） |
| DEF-012 | P1 | 空语音松手静默 | `fix-voice-empty.png` 显示「没听清，请再按住说一次」 | FIXED |
| DEF-013 | P0 | SiliconFlow FastExtract 全量 HTTP 402 | 产品侧 4xx 记 `http=402` 返回 null，不瞎写。账单/密钥仍 OPEN | CONFIRMED |

## 真机（MIX 3 · `com.yuyue.app.qa` · 2026-09-19）

个人包 `com.yuyue.app`（userId=10250）未改。QA userId=10194。

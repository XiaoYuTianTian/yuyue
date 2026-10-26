# Task 8 报告：差异化（differentiators）

## 状态

**DONE_WITH_CONCERNS**

## 目标（plan §7 第二批）

人物 / 地点 / 主题时间线、周/月回顾、照片墙；地图·天气·原声仅在本机数据可支撑时做低风险占位，不阻塞于网络 API。优先本地聚合， grounded 于 legacy `people` / `place` / `diaryType` 与 `entry_media`（可选合并 Domain Person/Place/Topic 链接）。

## 变更文件（主要）

| 文件 | 变更 |
|------|------|
| `ui/shell/DifferentiatorAggregate.kt` | 纯函数：人物/地点/主题分组、周/月回顾、照片墙/音频筛选 |
| `ui/shell/screens/DifferentiatorScreens.kt` | Facet 浏览/时间线、周月回顾、照片墙、LocalExtras 占位 + 入口芯片 |
| `ui/shell/Routes.kt` | `FacetBrowse` / `FacetTimeline` / `WeekReview` / `MonthReview` / `PhotoWall` / `LocalExtras` |
| `ui/shell/ShellViewModels.kt` | `DifferentiatorViewModel`（`listActiveEntries` + media） |
| `ui/shell/ShellDestinations.kt` / `YuyueNavHost.kt` | 路由与 Destination 接线 |
| `ui/shell/screens/VaultPaperScreen.kt` | 「浏览与回顾」卡片 |
| `ui/shell/screens/TimePaperScreen.kt` / `RecallPaperScreen.kt` | 同组入口芯片 |
| `data/YuyueRepository.kt` | `listActiveEntries()` / `listAllMedia()` |
| `test/.../DifferentiatorAggregateTest.kt` | 分组与聚合单测 |
| `test/.../ShellRoutesTest.kt` | 新路由 round-trip |
| `docs/rebuild/task-8-report.md` | 本报告 |

未改：计划文件、个人包数据、Domain DAO 写路径（仍旁路）。

## 各需求结论

| # | 项 | 结果 |
|---|----|------|
| 1 | 人物时间线 | **做**：`people` 分隔聚合 + 可选 `DomainNameLink` |
| 2 | 地点时间线 | **做**：`place` 字段 + 可选 Domain Place 链接 |
| 3 | 主题时间线 | **做**：`diaryType`（空→「未分类」）+ 可选 Topic 链接 |
| 4 | 周/月回顾 | **做**：本机按日聚合 + top 人/地/题 + 照片计数 |
| 5 | 照片墙 | **做**：`entry_media` 中 `image/*`，点开条目 |
| 6 | 地图/天气/原声 | **占位**：文本地点列表；天气说明不拉网；audio 列表可跳条目（未接 MediaPlayer） |
| 7 | 导航 | **做**：Vault + Time + Recall 入口 |
| 8 | 纯函数单测 | **做**：`DifferentiatorAggregateTest` |
| 9 | 本报告 | **做** |
| 10 | 全量单测 | **绿**（见下） |

## 测试

### 聚焦

```
.\gradlew.bat :android-app:testDebugUnitTest --offline --tests com.yuyue.app.ui.shell.DifferentiatorAggregateTest --tests com.yuyue.app.ui.shell.ShellRoutesTest
→ BUILD SUCCESSFUL
```

### 全量

```
.\gradlew.bat :android-app:testDebugUnitTest --offline
→ BUILD SUCCESSFUL (exit 0)
```

## 未决风险 / Concerns

1. **Domain 链接未自动灌入 UI**：`DomainNameLink` API 已支持合并，但 Person/Place/Topic DAO 目前几乎只有 upsert，运行时仍以 legacy 字符串为主。
2. **原声回放**：仅列出 `audio/*` 并跳转条目；真机 MediaPlayer 未接（避免无文件路径崩溃）。
3. **地图/天气**：无经纬度与天气快照字段；明确不调用外部 API。
4. **Facet 键大小写**：路由用 lowercase key；中文标签靠 `filterFacet` 兼容。
5. **回顾芯片行**：窄屏可能拥挤，未做横向滚动。

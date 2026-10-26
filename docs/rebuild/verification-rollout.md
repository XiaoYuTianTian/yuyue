# 验收与分阶段发布（Task 9）

## 状态

**DONE_WITH_CONCERNS** — JVM/Room 门禁可执行；真机矩阵与个人包升级需设备侧勾选。

## Oracle 顺序（不可颠倒）

1. Room / Domain / 导出文件证据  
2. 备份 checksum / YUY2 头  
3. UI 文案与截图  

没有 (1)(2) 不得宣称迁移或恢复成功。

## 已完成的自动化证据

| 门禁 | 命令 / 产物 | 结果 |
|------|-------------|------|
| 全量单元测试 | `.\gradlew.bat :android-app:testDebugUnitTest --offline` | 通过（本会话多次） |
| 备份契约 | BackupV2 / Archive / CryptoV2 | 通过 |
| Domain 导入 | LegacyV7ImporterTest | 通过 |
| CaptureKernel | CaptureKernelTest | 通过 |
| 金标 40×3 | `docs/testing/gold40.jsonl` + `Gold40HarnessTest` | L0 路由/拆条确定性 ×3 |

## 金标说明

- 40 条中文口语，覆盖 record / recall / mutate 与多事件拆条。  
- 跑 3 次要求确定性（`threeRunsAreDeterministic`）。  
- **误写率目标 0**：recall/mutate 不得走 Record 写入路径（由 CaptureKernel 单测 + 金标意图共同约束）。  
- 模型原文**不能**单独作为 PASS。

## 迁移演练清单（QA 包）

在 `com.yuyue.app.qa` 上执行，禁止触碰个人包数据：

1. 安装 QA → 写入 ≥5 条含媒体 → 导出 YUY2 加密备份 → 记录条目数/媒体数/checksum。  
2. 清空应用数据 → 试恢复预览（不写库）→ 核对预览计数。  
3. 正式恢复 → Room `entries` / `entry_media` 计数与备份一致。  
4. 跑 `LegacyV7Importer` 旁路到 Domain（开发构建）→ Capture.rawText 与条目数一致。  
5. RestoreAsCopy 再导一次 → 媒体 entryId 指向副本，旧条目媒体仍在。

## 真机矩阵（待勾选）

| 设备 | API | 冷启动 | 按住说 | 分享图 | 小组件 | 锁 | 那年今日 | 备份恢复 |
|------|-----|--------|--------|--------|--------|----|----------|----------|
| 模拟器 x86 | 34 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| ARM 真机 A |  | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| ARM 真机 B |  | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |

性能门槛：原话持久化 ≤300ms（CaptureKernel 单测已断言本地路径）；冷启动与月历不等待模型。

## 发布梯度

1. JVM/Room 全绿（已完成）  
2. QA 模拟器 E2E  
3. ARM 真机矩阵  
4. QA 迁移与恢复演练签字  
5. **个人包升级前强制加密备份** → 升级 → 抽查计数  
6. 小范围发布；一句话定位：**按住说一句，落到那一天**

## 回滚

- `FEATURE_REBUILD_SHELL=false` → LegacyNavHost  
- 保留 `yuyue.db` v7；Domain DB 未接管写入前可单独删除 `yuyue-domain.db`

## 未决（设备/商店）

- Biometric Prompt 未接系统对话框  
- 中文 PDF 字体、OEM 通知权限、相册选择器需真机  
- Domain 写入切流与个人包一次升级验证尚未执行  

> 代码收尾总览见 [`REBUILD-COMPLETE.md`](REBUILD-COMPLETE.md)。Domain dual-write 与 Kernel 独占写入已在 Task 10–13 落地；上表仍指**真机/发布**门禁。

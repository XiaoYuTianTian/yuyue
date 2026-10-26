# Task 9 报告：验收与发布门禁

## 状态

**DONE_WITH_CONCERNS**

## 变更

| 路径 | 说明 |
|------|------|
| `docs/testing/gold40.jsonl` | 补齐 40 条中文口语金标 |
| `android-app/.../Gold40HarnessTest.kt` | L0 路由/拆条 ×3 确定性 |
| `docs/rebuild/verification-rollout.md` | 迁移清单、真机矩阵、发布梯度 |
| `docs/rebuild/task-9-report.md` | 本报告 |

## 测试

```
.\gradlew.bat :android-app:testDebugUnitTest --offline --tests com.yuyue.app.llm.Gold40HarnessTest
.\gradlew.bat :android-app:testDebugUnitTest --offline
```

## 风险

- 真机矩阵与个人包升级未在本环境执行。  
- 金标仅覆盖 LocalRouter/MultiEventSplit，不含云模型路径。  
- Domain 尚未切为默认写库。  
